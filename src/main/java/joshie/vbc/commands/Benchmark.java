package joshie.vbc.commands;

import joshie.vbc.config.FlatPenaltyCache;
import joshie.vbc.config.VillagerGlobalTickCache;
import joshie.vbc.utils.MalusUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static joshie.vbc.VillagerBrainConfig.LOGGER;

final class Benchmark {
    private static final int RADIUS = 16;
    private static final int MAX_TARGETS = 64;

    private record PathResult(double averageMs, double minMs, int successes) {
    }

    private Benchmark() {
    }

    static String run(ServerLevel level, BlockPos origin, int iterations) {
        List<BlockPos> targets = sampleTargets(level, origin);
        if (targets.size() < 8) {
            return "benchmark skipped: only " + targets.size() + " standable targets within " + RADIUS + " blocks of " + origin;
        }
        Set<BlockPos> targetSet = new HashSet<>(targets);

        Villager unprofessed = new Villager(EntityType.VILLAGER, level);
        Villager farmer = new Villager(EntityType.VILLAGER, level);
        farmer.setVillagerData(farmer.getVillagerData().setProfession(VillagerProfession.FARMER));
        Zombie zombie = new Zombie(EntityType.ZOMBIE, level);

        for (PathfinderMob mob : new PathfinderMob[] {unprofessed, farmer, zombie}) {
            mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16.0);
        }

        Block[] below = new Block[targets.size()];
        Block[] at = new Block[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            BlockPos pos = targets.get(i);
            below[i] = level.getBlockState(pos.below()).getBlock();
            at[i] = level.getBlockState(pos).getBlock();
        }

        long callsBefore = VillagerGlobalTickCache.malusCallsTotal;

        PathResult villagerNone = measurePaths(unprofessed, origin, targetSet, iterations);
        PathResult villagerFarmer = measurePaths(farmer, origin, targetSet, iterations);
        PathResult zombieResult = measurePaths(zombie, origin, targetSet, iterations);
        String workload = workload(iterations * 2, VillagerGlobalTickCache.malusCallsTotal - callsBefore);

        return String.join("\n",
                "benchmark origin=" + origin.toShortString() + " targets=" + targets.size() + " iterations=" + iterations,
                "benchmark villager-none   " + format(villagerNone),
                "benchmark villager-farmer " + format(villagerFarmer),
                "benchmark zombie-vanilla  " + format(zombieResult),
                "benchmark " + workload,
                "benchmark " + measureMalus(unprofessed, targets, below, at));
    }

    private static String workload(int paths, long calls) {
        return String.format("workload malusCalls=%d perPath=%.1f", calls, calls / (double) Math.max(1, paths));
    }

    private static String format(PathResult result) {
        return String.format("avg=%.3fms min=%.3fms paths=%d",
                result.averageMs(), result.minMs(), result.successes());
    }

    private static PathResult measurePaths(PathfinderMob mob, BlockPos origin, Set<BlockPos> targets, int iterations) {
        PathNavigation navigation = mob.getNavigation();
        for (int i = 0; i < 3; i++) {
            reset(mob, origin);
            navigation.createPath(targets, 1);
        }

        long total = 0L;
        long best = Long.MAX_VALUE;
        int successes = 0;
        for (int i = 0; i < iterations; i++) {
            reset(mob, origin);
            long start = System.nanoTime();
            Path path = navigation.createPath(targets, 1);
            long elapsed = System.nanoTime() - start;
            total += elapsed;
            best = Math.min(best, elapsed);
            if (path != null) {
                successes++;
            }
        }
        return new PathResult(total / 1.0e6 / iterations, best / 1.0e6, successes);
    }

    private static void reset(PathfinderMob mob, BlockPos origin) {
        mob.getNavigation().stop();
        mob.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        mob.setOnGround(true);
    }

    private static String measureMalus(Villager villager, List<BlockPos> targets, Block[] below, Block[] at) {
        int rounds = 40;
        int calls = rounds * targets.size();
        long warm = timeMalus(rounds, targets, villager, false);
        long cold = timeMalus(rounds, targets, villager, true);

        long vanillaStart = System.nanoTime();
        for (int i = 0; i < calls; i++) {
            villager.getPathfindingMalus(PathType.WALKABLE);
        }
        long vanilla = System.nanoTime() - vanillaStart;

        long tableStart = System.nanoTime();
        int tableCalls = 0;
        for (int round = 0; round < rounds; round++) {
            for (int i = 0; i < targets.size(); i++) {
                FlatPenaltyCache.getWalkOn(below[i], -1);
                FlatPenaltyCache.getWalkThrough(at[i], -1);
                tableCalls += 2;
            }
        }
        long table = System.nanoTime() - tableStart;

        return String.format("malus warm=%.0fns cold=%.0fns table=%.0fns vanillaRead=%.1fns",
                (double) warm / calls, (double) cold / calls, (double) table / tableCalls, (double) vanilla / calls);
    }

    private static long timeMalus(int rounds, List<BlockPos> targets, Villager villager, boolean cold) {
        long start = System.nanoTime();
        for (int round = 0; round < rounds; round++) {
            if (cold) {
                VillagerGlobalTickCache.reset();
            }
            for (BlockPos pos : targets) {
                MalusUtils.maybeVillagerMalus(null, PathType.WALKABLE, villager, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return System.nanoTime() - start;
    }

    private static List<BlockPos> sampleTargets(ServerLevel level, BlockPos origin) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                level.getChunk(x >> 4, z >> 4);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - origin.getY()) > 8) {
                    continue;
                }
                cursor.set(x, y, z);
                if (!level.getBlockState(cursor).isAir()) {
                    continue;
                }
                BlockPos floorPos = cursor.below();
                BlockState floor = level.getBlockState(floorPos);
                if (floor.is(Blocks.COBWEB) || floor.getCollisionShape(level, floorPos).isEmpty()) {
                    continue;
                }
                found.add(cursor.immutable());
            }
        }
        found.sort(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        if (found.size() <= MAX_TARGETS) {
            return found;
        }
        List<BlockPos> sampled = new ArrayList<>(MAX_TARGETS);
        double step = found.size() / (double) MAX_TARGETS;
        for (int i = 0; i < MAX_TARGETS; i++) {
            sampled.add(found.get((int) (i * step)));
        }
        return sampled;
    }

    static void log(String body) {
        for (String line : body.split("\n")) {
            LOGGER.info(line);
        }
    }
}
