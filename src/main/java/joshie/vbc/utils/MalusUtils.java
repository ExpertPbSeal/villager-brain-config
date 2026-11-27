package joshie.vbc.utils;

import joshie.vbc.config.ConfigManager;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MalusUtils {
    public static boolean hasMalus(PathfinderMob mob, BlockPos pos) {
        return false;
    }

    public static String getProfessionId(Villager villager) {
        return joshie.vbc.config.ProfessionCache.getProfessionId(villager);
    }



    public static float maybeVillagerMalus(NodeEvaluator evaluator, PathType pathType, Mob mob, int x, int y, int z) {
        if (pathType == PathType.BLOCKED) {
            return -1.0F;
        }
        
        long startTime = PerformanceTracker.startTimer();
        
        try {
            if (mob instanceof Villager villager) {
                int professionIndex = FlatPenaltyCache.indexOf(getProfessionId(villager));
                BlockPos.MutableBlockPos pos = BlockPosPool.acquire1();
                pos.set(x, y, z);
                
                float[] parts = VillagerGlobalTickCache.getPenaltyParts(pos, professionIndex,
                        () -> computeParts(mob, x, y, z, professionIndex));
                
                return merge(parts[0], parts[1],
                        FlatPenaltyCache.getWalkOnDefault(professionIndex),
                        FlatPenaltyCache.getWalkThroughDefault(professionIndex),
                        parts[2], mob.getPathfindingMalus(pathType));
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

        return new float[] { walkOn, walkThrough, avoid };
    }

    public static float merge(float walkOn, float walkThrough, float walkOnDefault, float walkThroughDefault,
                              float avoid, float vanilla) {
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
        }

        if (Float.isNaN(walkThrough)) {
            total += walkThroughDefault;
            missed = true;
        } else {
            total += walkThrough;
        }

        if (!missed) {
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
        List<ProcessedAvoidEntry> avoids = FlatPenaltyCache.getAvoids(professionIndex);
        if (avoids.isEmpty()) return 0;

        double total = 0;
        Set<ProcessedAvoidEntry> matched = new HashSet<>();

        int maxRadius = avoids.stream().mapToInt(a -> a.radius).max().orElse(0);
        int y0 = nodePos.getY();

        for (int y = y0; y <= y0 + 2; y++) {
            for (int dx = -maxRadius; dx <= maxRadius; dx++) {
                for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                    double distSq = dx * dx + dz * dz;
                    BlockPos checkPos = nodePos.offset(dx, y - y0, dz);
                    Block block = level.getBlockState(checkPos).getBlock();

                    for (ProcessedAvoidEntry avoid : avoids) {
                        if (matched.contains(avoid)) continue;
                        if (distSq > avoid.radius * avoid.radius) continue;

                        if (avoid.matches(block)) {
                            total += avoid.penalty;
                            matched.add(avoid);
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
        String professionId = getProfessionId(villager);
        return ConfigManager.getActionPenalty(professionId, "jump");
    }
}


