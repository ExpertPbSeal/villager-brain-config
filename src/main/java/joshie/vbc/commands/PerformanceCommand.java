package joshie.vbc.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import joshie.vbc.config.FlatPenaltyCache;
import joshie.vbc.config.ProcessedAvoidEntry;
import joshie.vbc.config.ProfessionCache;
import joshie.vbc.config.TickLocalCache;
import joshie.vbc.config.VillagerGlobalTickCache;
import joshie.vbc.utils.BlockPosPool;
import joshie.vbc.utils.MalusUtils;
import joshie.vbc.utils.PerformanceTracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.List;

public class PerformanceCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villager_brain_config")
                .then(Commands.literal("performance")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            
                            PerformanceTracker.PerformanceStats perfStats = PerformanceTracker.getStats();
                            FlatPenaltyCache.CacheStats flatStats = FlatPenaltyCache.getStats();
                            ProfessionCache.CacheStats professionStats = ProfessionCache.getStats();
                            TickLocalCache.CacheStats tickStats = TickLocalCache.getStats();
                            VillagerGlobalTickCache.CacheStats globalStats = VillagerGlobalTickCache.getStats();
                            BlockPosPool.PoolStats poolStats = BlockPosPool.getStats();
                            
                            source.sendSuccess(() -> Component.literal("=== Villager Brain Config Performance Stats ==="), false);
                            
                            source.sendSuccess(() -> Component.literal(String.format(
                                "Malus calls: %d (avg: %.3fms)", 
                                perfStats.malusCallCount, perfStats.averageMalusTimeMs)), false);
                            source.sendSuccess(() -> Component.literal(String.format(
                                "Penalty lookups: %d (avg: %.3fms)", 
                                perfStats.penaltyLookupCount, perfStats.averagePenaltyTimeMs)), false);
                            
                            source.sendSuccess(() -> Component.literal(String.format(
                                "Flat penalty cache: professions=%d, entries=%d, global=%s", 
                                flatStats.professionsLoaded, flatStats.totalEntries, flatStats.globalPenaltiesLoaded)), false);
                            source.sendSuccess(() -> Component.literal(String.format(
                                "Profession cache: valid=%d, total=%d (%.1f%% hit rate)", 
                                professionStats.validEntries, professionStats.totalEntries,
                                professionStats.totalEntries > 0 ? 
                                    (professionStats.validEntries * 100.0 / professionStats.totalEntries) : 0.0)), false);
                            source.sendSuccess(() -> Component.literal(String.format(
                                "Tick-local cache: size=%d, tick=%d", 
                                tickStats.cacheSize, tickStats.lastTick)), false);
                            source.sendSuccess(() -> Component.literal(String.format(
                                "🚀 GLOBAL tick cache: malus=%d(%.1f%%), nodeType=%d(%.1f%%), tick=%d", 
                                globalStats.malusCacheSize, globalStats.getMalusHitRate(),
                                globalStats.nodeTypeCacheSize, globalStats.getNodeTypeHitRate(),
                                globalStats.currentTick)), false);
                            source.sendSuccess(() -> Component.literal(String.format(
                                "ThreadLocal BlockPos pool: ~%d instances", 
                                poolStats.estimatedInstances)), false);
                            
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("clear-cache")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            TickLocalCache.clear();
                            VillagerGlobalTickCache.reset();
                            BlockPosPool.clear();
                            
                            ctx.getSource().sendSuccess(() -> Component.literal("Tick caches cleared, penalty snapshot kept"), true);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("reset-stats")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            PerformanceTracker.reset();
                            
                            ctx.getSource().sendSuccess(() -> Component.literal("Performance statistics reset"), true);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("dump")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("block", ResourceLocationArgument.id())
                                .executes(ctx -> dump(ctx.getSource(), ResourceLocationArgument.getId(ctx, "block"), null, null))
                                .then(Commands.argument("profession", ResourceLocationArgument.id())
                                        .executes(ctx -> dump(ctx.getSource(), ResourceLocationArgument.getId(ctx, "block"),
                                                ResourceLocationArgument.getId(ctx, "profession").toString(), null))
                                        .then(Commands.argument("path_type", StringArgumentType.word())
                                                .executes(ctx -> dump(ctx.getSource(), ResourceLocationArgument.getId(ctx, "block"),
                                                        ResourceLocationArgument.getId(ctx, "profession").toString(),
                                                        StringArgumentType.getString(ctx, "path_type"))))))));
    }

    private static int dump(CommandSourceStack source, ResourceLocation blockId, String profession, String pathTypeName) {
        if (!BuiltInRegistries.BLOCK.containsKey(blockId)) {
            source.sendFailure(Component.literal("Unknown block " + blockId));
            return 0;
        }

        PathType resolvedPathType = pathTypeName == null ? null : parsePathType(pathTypeName);
        if (pathTypeName != null && resolvedPathType == null) {
            source.sendFailure(Component.literal("Unknown path type " + pathTypeName));
            return 0;
        }
        final PathType pathType = resolvedPathType;

        Block block = BuiltInRegistries.BLOCK.get(blockId);
        int index = profession == null ? -1 : FlatPenaltyCache.indexOf(profession);

        source.sendSuccess(() -> Component.literal(String.format("dump %s profession=%s",
                blockId, profession == null ? "none" : profession)), false);
        if (profession != null && !FlatPenaltyCache.hasProfession(profession)) {
            source.sendSuccess(() -> Component.literal("profession is not configured, using the global layer"), false);
        }

        source.sendSuccess(() -> Component.literal(String.format("walk_on=%s (%s) default=%s",
                value(FlatPenaltyCache.getWalkOn(block, index)),
                FlatPenaltyCache.layerName(FlatPenaltyCache.getWalkOnLayer(block, index)),
                FlatPenaltyCache.getWalkOnDefault(index))), false);
        source.sendSuccess(() -> Component.literal(String.format("walk_through=%s (%s) default=%s",
                value(FlatPenaltyCache.getWalkThrough(block, index)),
                FlatPenaltyCache.layerName(FlatPenaltyCache.getWalkThroughLayer(block, index)),
                FlatPenaltyCache.getWalkThroughDefault(index))), false);

        List<ProcessedAvoidEntry> avoids = FlatPenaltyCache.getAvoids(index);
        source.sendSuccess(() -> Component.literal(String.format("avoids=%d merged", avoids.size())), false);
        for (ProcessedAvoidEntry avoid : avoids) {
            source.sendSuccess(() -> Component.literal(String.format("  %s radius=%d penalty=%s",
                    avoid.key, avoid.radius, avoid.penalty)), false);
        }

        if (pathType != null) {
            BlockPos origin = BlockPos.containing(source.getPosition());
            float avoid = (float) MalusUtils.computeAvoidPenalty(source.getLevel(), index, origin);
            Villager probe = new Villager(EntityType.VILLAGER, source.getLevel());
            float vanilla = probe.getPathfindingMalus(pathType);
            float merged = MalusUtils.merge(
                    FlatPenaltyCache.getWalkOn(block, index),
                    FlatPenaltyCache.getWalkThrough(block, index),
                    FlatPenaltyCache.getWalkOnDefault(index),
                    FlatPenaltyCache.getWalkThroughDefault(index),
                    avoid, vanilla,
                    FlatPenaltyCache.getWalkOnLayer(block, index),
                    FlatPenaltyCache.getWalkThroughLayer(block, index));
            source.sendSuccess(() -> Component.literal(String.format(
                    "path_type=%s vanilla=%s avoid=%s merged=%s", pathType, vanilla, avoid, merged)), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static PathType parsePathType(String name) {
        for (PathType pathType : PathType.values()) {
            if (pathType.name().equalsIgnoreCase(name)) {
                return pathType;
            }
        }
        return null;
    }

    private static String value(float value) {
        return Float.isNaN(value) ? "miss" : String.valueOf(value);
    }
}