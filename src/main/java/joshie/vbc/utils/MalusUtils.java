package joshie.vbc.utils;

import joshie.vbc.config.FlatPenaltyCache;
import joshie.vbc.config.ProcessedAvoidEntry;
import joshie.vbc.config.VillagerGlobalTickCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;

import javax.annotation.Nullable;


public class MalusUtils {
    public static boolean hasMalus(PathfinderMob mob, BlockPos pos) {
        return false;
    }

    public static String getProfessionId(Villager villager) {
        return joshie.vbc.config.ProfessionCache.getProfessionId(villager);
    }

    private static int professionIndexOf(Villager villager) {
        int index = FlatPenaltyCache.indexOf(villager.getVillagerData().getProfession());
        if (index >= 0) {
            return index;
        }
        return FlatPenaltyCache.indexOf(getProfessionId(villager));
    }



    public static float maybeVillagerMalus(@Nullable NodeEvaluator evaluator, PathType pathType, Mob mob, int x, int y, int z) {
        if (pathType == PathType.BLOCKED) {
            return -1.0F;
        }
        
        long startTime = PerformanceTracker.startTimer();
        
        try {
            if (mob instanceof Villager villager) {
                int professionIndex = professionIndexOf(villager);
                BlockPos.MutableBlockPos pos = BlockPosPool.acquire1();
                pos.set(x, y, z);
                
                float[] parts = VillagerGlobalTickCache.getPenaltyParts(pos, professionIndex,
                        () -> computeParts(mob, x, y, z, professionIndex));
                
                return merge(parts[0], parts[1],
                        FlatPenaltyCache.getWalkOnDefault(professionIndex),
                        FlatPenaltyCache.getWalkThroughDefault(professionIndex),
                        parts[2], mob.getPathfindingMalus(pathType),
                        (byte) parts[3], (byte) parts[4]);
            }
            
            return mob.getPathfindingMalus(pathType);
        } finally {
            if (startTime > 0) {
                PerformanceTracker.recordMalusCall(System.nanoTime() - startTime);
            }
        }
    }

    private static float[] computeParts(Mob mob, int x, int y, int z, int professionIndex) {
        Level level = mob.level();
        BlockPos.MutableBlockPos posBelow = BlockPosPool.acquire2();
        BlockPos.MutableBlockPos posAt = BlockPosPool.acquire3();

        posBelow.set(x, y - 1, z);
        posAt.set(x, y, z);

        Block blockBelow = level.getBlockState(posBelow).getBlock();
        Block blockAt = level.getBlockState(posAt).getBlock();

        float walkOn = FlatPenaltyCache.getWalkOn(blockBelow, professionIndex);
        float walkThrough = FlatPenaltyCache.getWalkThrough(blockAt, professionIndex);
        float avoid = level instanceof ServerLevel serverLevel
                ? (float) computeAvoidPenalty(serverLevel, professionIndex, posAt)
                : 0.0F;

        byte walkOnLayer = Float.isNaN(walkOn) ? FlatPenaltyCache.LAYER_NONE : FlatPenaltyCache.getWalkOnLayer(blockBelow, professionIndex);
        byte walkThroughLayer = Float.isNaN(walkThrough) ? FlatPenaltyCache.LAYER_NONE : FlatPenaltyCache.getWalkThroughLayer(blockAt, professionIndex);

        return new float[] { walkOn, walkThrough, avoid, walkOnLayer, walkThroughLayer };
    }

    public static float merge(float walkOn, float walkThrough, float walkOnDefault, float walkThroughDefault,
                              float avoid, float vanilla, byte walkOnLayer, byte walkThroughLayer) {
        if (walkOn < 0.0F || walkThrough < 0.0F || avoid < 0.0F) {
            return -1.0F;
        }

        float total = avoid;
        boolean missed = false;

        if (Float.isNaN(walkOn)) {
            total += walkOnDefault;
            missed = true;
        } else {
            total += walkOn;
            if (walkOn == 0.0F) missed = true;
        }

        if (Float.isNaN(walkThrough)) {
            total += walkThroughDefault;
            missed = true;
        } else {
            total += walkThrough;
            if (walkThrough == 0.0F) missed = true;
        }

        if (!missed) {
            if (vanilla < 0.0F && (FlatPenaltyCache.isTagLayer(walkOnLayer) || FlatPenaltyCache.isTagLayer(walkThroughLayer))) {
                return -1.0F;
            }
            return total;
        }
        if (vanilla < 0.0F) {
            return -1.0F;
        }
        return Math.max(total, vanilla);
    }

    public static double getAvoidPenalty(Mob mob, BlockPos nodePos) {
        if (!(mob instanceof Villager villager)) return 0;
        if (!(mob.level() instanceof ServerLevel serverLevel)) return 0;

        return computeAvoidPenalty(serverLevel, FlatPenaltyCache.indexOf(getProfessionId(villager)), nodePos);
    }

    public static double computeAvoidPenalty(ServerLevel level, int professionIndex, BlockPos nodePos) {
        FlatPenaltyCache.AvoidData data = FlatPenaltyCache.getAvoidData(professionIndex);
        int count = data.entries.size();
        if (count == 0) return 0;

        boolean[] matched = new boolean[count];
        BlockPos.MutableBlockPos check = BlockPosPool.acquire4();

        int x = nodePos.getX();
        int y0 = nodePos.getY();
        int z = nodePos.getZ();
        int maxRadius = data.maxRadius;
        int maxRadiusSq = maxRadius * maxRadius;
        double total = 0;

        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -maxRadius; dx <= maxRadius; dx++) {
                for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq > maxRadiusSq) continue;

                    check.set(x + dx, y0 + dy, z + dz);
                    Block block = level.getBlockState(check).getBlock();
                    int[] candidates = data.byBlock.get(block);
                    if (candidates == null) continue;

                    for (int index : candidates) {
                        if (matched[index]) continue;

                        ProcessedAvoidEntry avoid = data.entries.get(index);
                        if (distSq > avoid.radius * avoid.radius) continue;

                        total += avoid.penalty;
                        matched[index] = true;

                        if (data.allNonPositive && avoid.penalty < 0.0) {
                            return -1.0;
                        }
                    }
                }
            }
        }
        return total;
    }

    public static float getJumpPenalty(Mob mob) {
        if (!(mob instanceof Villager villager)) {
            return 0.0f;
        }
        return FlatPenaltyCache.getJumpPenalty(professionIndexOf(villager));
    }
}


