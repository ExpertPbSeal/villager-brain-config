package joshie.vbc.config;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static joshie.vbc.VillagerBrainConfig.LOGGER;


public final class FlatPenaltyCache {
    public static final float CONSTANT_WALK_ON = 4.0F;
    public static final float CONSTANT_WALK_THROUGH = 0.0F;

    public static final byte LAYER_NONE = 0;
    public static final byte LAYER_PROFESSION_EXACT = 1;
    public static final byte LAYER_PROFESSION_TAG = 2;
    public static final byte LAYER_GLOBAL_EXACT = 3;
    public static final byte LAYER_GLOBAL_TAG = 4;
    public static final byte LAYER_GLOBAL_DEFAULT = 5;

    private static final String[] LAYER_NAMES = {
            "none", "profession-exact", "profession-tag", "global-exact", "global-tag", "global-default"
    };

    private static volatile Snapshot snapshot = new Snapshot(
            Collections.emptyMap(), new Table[0], Table.EMPTY, Collections.emptyList(), Collections.emptyList());

    private FlatPenaltyCache() {
    }
    

    public static void rebuild(RegistryAccess registryAccess) {
        long started = System.nanoTime();
        Registry<Block> blocks = registryAccess.registryOrThrow(Registries.BLOCK);
        Diagnostics diagnostics = new Diagnostics();

        Table global = buildTable(blocks,
                ConfigManager.CONFIG.walkOnBlockPenalties,
                ConfigManager.CONFIG.walkThroughBlockPenalties,
                null, diagnostics, "global");

        Map<String, Integer> index = new HashMap<>();
        List<Table> professions = new ArrayList<>();
        List<List<ProcessedAvoidEntry>> avoids = new ArrayList<>();
        List<ProcessedAvoidEntry> globalAvoids = readAvoids(blocks, ConfigManager.CONFIG.avoids, diagnostics, "global");

        for (Map.Entry<String, ModConfig> entry : ConfigManager.CONFIG.professions.entrySet()) {
            ModConfig config = entry.getValue() == null ? new ModConfig() : entry.getValue();
            index.put(entry.getKey(), professions.size());
            professions.add(buildTable(blocks, config.walkOnBlockPenalties, config.walkThroughBlockPenalties,
                    global, diagnostics, entry.getKey()));
            avoids.add(mergeAvoids(readAvoids(blocks, config.avoids, diagnostics, entry.getKey()), globalAvoids));
        }

        snapshot = new Snapshot(index, professions.toArray(new Table[0]), global, avoids, globalAvoids);
        VillagerGlobalTickCache.reset();

        LOGGER.info("Penalty snapshot rebuilt: professions={}, blocks={}, tags={}, unresolved={}, emptyTags={}, time={}ms",
                professions.size(), global.cells.size(), diagnostics.tags, diagnostics.unresolved, diagnostics.emptyTags,
                (System.nanoTime() - started) / 1_000_000L);
    }

    private static Table buildTable(Registry<Block> blocks, Map<String, Double> walkOn, Map<String, Double> walkThrough,
                                    Table fallback, Diagnostics diagnostics, String owner) {
        byte exactLayer = fallback == null ? LAYER_GLOBAL_EXACT : LAYER_PROFESSION_EXACT;
        byte tagLayer = fallback == null ? LAYER_GLOBAL_TAG : LAYER_PROFESSION_TAG;

        TypeTable walkOnType = readType(blocks, walkOn, CONSTANT_WALK_ON, exactLayer, tagLayer, diagnostics, owner, "walk_on");
        TypeTable walkThroughType = readType(blocks, walkThrough, CONSTANT_WALK_THROUGH, exactLayer, tagLayer, diagnostics, owner, "walk_through");

        Reference2ObjectMap<Block, Hit> walkOnHits = resolve(blocks, walkOnType);
        Reference2ObjectMap<Block, Hit> walkThroughHits = resolve(blocks, walkThroughType);

        Reference2ObjectMap<Block, Cell> cells = new Reference2ObjectOpenHashMap<>();
        for (Block block : blocks) {
            Hit walkOnHit = walkOnHits.get(block);
            Hit walkThroughHit = walkThroughHits.get(block);
            if (walkOnHit == null && walkThroughHit == null) continue;
            cells.put(block, new Cell(
                    walkOnHit == null ? Float.NaN : walkOnHit.value,
                    walkOnHit == null ? LAYER_NONE : walkOnHit.layer,
                    walkThroughHit == null ? Float.NaN : walkThroughHit.value,
                    walkThroughHit == null ? LAYER_NONE : walkThroughHit.layer));
        }

        float walkOnDefault = fallback == null ? walkOnType.defaultValue : fallback.walkOnDefault;
        float walkThroughDefault = fallback == null ? walkThroughType.defaultValue : fallback.walkThroughDefault;

        if (fallback == null) {
            return new Table(cells, walkOnDefault, walkThroughDefault);
        }

        Reference2ObjectMap<Block, Cell> merged = new Reference2ObjectOpenHashMap<>(cells.size() + fallback.cells.size());
        for (Block block : blocks) {
            Cell own = cells.get(block);
            Cell base = fallback.cells.get(block);
            if (own == null) {
                if (base != null) merged.put(block, base);
            } else if (base == null) {
                merged.put(block, own);
            } else {
                merged.put(block, new Cell(
                        Float.isNaN(own.walkOn) ? base.walkOn : own.walkOn,
                        Float.isNaN(own.walkOn) ? base.walkOnLayer : own.walkOnLayer,
                        Float.isNaN(own.walkThrough) ? base.walkThrough : own.walkThrough,
                        Float.isNaN(own.walkThrough) ? base.walkThroughLayer : own.walkThroughLayer));
            }
        }
        return new Table(merged, walkOnDefault, walkThroughDefault);
    }


    public static int indexOf(String profession) {
        Integer index = snapshot.index.get(profession);
        return index == null ? -1 : index;
    }

    public static boolean hasProfession(String profession) {
        return snapshot.index.containsKey(profession);
    }

    public static float getWalkOn(Block block, int professionIndex) {
        Cell cell = cell(block, professionIndex);
        return cell == null ? Float.NaN : cell.walkOn;
    }

    public static float getWalkThrough(Block block, int professionIndex) {
        Cell cell = cell(block, professionIndex);
        return cell == null ? Float.NaN : cell.walkThrough;
    }

    public static float getWalkOnDefault(int professionIndex) {
        return table(professionIndex).walkOnDefault;
    }

    public static float getWalkThroughDefault(int professionIndex) {
        return table(professionIndex).walkThroughDefault;
    }

    public static byte getWalkOnLayer(Block block, int professionIndex) {
        Cell cell = cell(block, professionIndex);
        return cell == null || Float.isNaN(cell.walkOn) ? LAYER_GLOBAL_DEFAULT : cell.walkOnLayer;
    }

    public static byte getWalkThroughLayer(Block block, int professionIndex) {
        Cell cell = cell(block, professionIndex);
        return cell == null || Float.isNaN(cell.walkThrough) ? LAYER_GLOBAL_DEFAULT : cell.walkThroughLayer;
    }

    public static String layerName(byte layer) {
        return LAYER_NAMES[layer];
    }

    public static List<ProcessedAvoidEntry> getAvoids(int professionIndex) {
        Snapshot current = snapshot;
        if (professionIndex < 0 || professionIndex >= current.avoids.size()) return current.globalAvoids;
        return current.avoids.get(professionIndex);
    }

    private static Cell cell(Block block, int professionIndex) {
        if (block == null) return null;
        return table(professionIndex).cells.get(block);
    }

    private static Table table(int professionIndex) {
        Snapshot current = snapshot;
        if (professionIndex < 0 || professionIndex >= current.professions.length) return current.global;
        return current.professions[professionIndex];
    }

    public static CacheStats getStats() {
        Snapshot current = snapshot;
        int cells = current.global.cells.size();
        for (Table table : current.professions) {
            cells += table.cells.size();
        }
        return new CacheStats(current.professions.length, cells, !current.global.cells.isEmpty());
    }

    private static TypeTable readType(Registry<Block> blocks, Map<String, Double> penalties, float constant,
                                      byte exactLayer, byte tagLayer, Diagnostics diagnostics, String owner, String type) {
        TypeTable table = new TypeTable(constant, exactLayer, tagLayer);
        if (penalties == null) return table;

        for (Map.Entry<String, Double> entry : penalties.entrySet()) {
            String key = entry.getKey();
            if (entry.getValue() == null) {
                diagnostics.invalid(owner, type, key);
                continue;
            }
            float value = entry.getValue().floatValue();
            if ("default".equals(key)) {
                table.defaultValue = value;
            } else if (key.startsWith("#")) {
                ResourceLocation id = namespaced(key) ? ResourceLocation.tryParse(key.substring(1)) : null;
                if (id == null) {
                    diagnostics.invalid(owner, type, key);
                } else {
                    table.tags.add(new TagRule(id, value, key));
                }
            } else {
                ResourceLocation id = namespaced(key) ? ResourceLocation.tryParse(key) : null;
                if (id == null) {
                    diagnostics.invalid(owner, type, key);
                } else if (!blocks.containsKey(id)) {
                    diagnostics.unknown(owner, type, key);
                } else {
                    table.exact.put(blocks.get(id), value);
                }
            }
        }

        table.tags.sort(Comparator.comparing(rule -> rule.key));
        for (TagRule rule : table.tags) {
            int members = 0;
            for (Block block : blocks) {
                if (rule.matches(block)) members++;
            }
            if (members == 0) {
                diagnostics.emptyTag(owner, type, rule.key);
            } else {
                diagnostics.tag();
            }
        }
        return table;
    }

    private static Reference2ObjectMap<Block, Hit> resolve(Registry<Block> blocks, TypeTable type) {
        Reference2ObjectMap<Block, Hit> hits = new Reference2ObjectOpenHashMap<>();
        for (Reference2ObjectMap.Entry<Block, Float> entry : type.exact.reference2ObjectEntrySet()) {
            hits.put(entry.getKey(), new Hit(entry.getValue(), type.exactLayer));
        }
        if (type.tags.isEmpty()) return hits;

        for (Block block : blocks) {
            if (hits.containsKey(block)) continue;
            for (TagRule rule : type.tags) {
                if (rule.matches(block)) {
                    hits.put(block, new Hit(rule.value, type.tagLayer));
                    break;
                }
            }
        }
        return hits;
    }

    private static List<ProcessedAvoidEntry> readAvoids(Registry<Block> blocks, List<AvoidEntry> entries,
                                                        Diagnostics diagnostics, String owner) {
        List<ProcessedAvoidEntry> result = new ArrayList<>();
        if (entries == null) return result;

        for (AvoidEntry avoid : entries) {
            if (avoid == null || avoid.block == null) {
                diagnostics.invalidAvoid(owner, String.valueOf(avoid == null ? null : avoid.block));
                continue;
            }
            ProcessedAvoidEntry processed = new ProcessedAvoidEntry();
            processed.key = avoid.block;
            processed.radius = avoid.radius;
            processed.penalty = avoid.penalty;
            if (avoid.block.startsWith("#")) {
                ResourceLocation id = namespaced(avoid.block) ? ResourceLocation.tryParse(avoid.block.substring(1)) : null;
                if (id == null) {
                    diagnostics.invalidAvoid(owner, avoid.block);
                    continue;
                }
                processed.isTag = true;
                processed.tag = TagKey.create(Registries.BLOCK, id);
            } else {
                ResourceLocation id = namespaced(avoid.block) ? ResourceLocation.tryParse(avoid.block) : null;
                if (id == null || !blocks.containsKey(id)) {
                    diagnostics.invalidAvoid(owner, avoid.block);
                    continue;
                }
                processed.block = blocks.get(id);
            }
            result.add(processed);
        }
        return result;
    }

    private static List<ProcessedAvoidEntry> mergeAvoids(List<ProcessedAvoidEntry> own, List<ProcessedAvoidEntry> global) {
        if (global.isEmpty()) return own;
        if (own.isEmpty()) return global;

        List<ProcessedAvoidEntry> merged = new ArrayList<>(own.size() + global.size());
        Set<String> keys = new HashSet<>();
        for (ProcessedAvoidEntry entry : own) {
            merged.add(entry);
            keys.add(entry.key);
        }
        for (ProcessedAvoidEntry entry : global) {
            if (keys.add(entry.key)) merged.add(entry);
        }
        return merged;
    }

    private static boolean namespaced(String key) {
        int colon = key.indexOf(':');
        return colon > 0 && colon < key.length() - 1;
    }

    public static class CacheStats {
        public final int professionsLoaded;
        public final int totalEntries;
        public final boolean globalPenaltiesLoaded;

        CacheStats(int professionsLoaded, int totalEntries, boolean globalPenaltiesLoaded) {
            this.professionsLoaded = professionsLoaded;
            this.totalEntries = totalEntries;
            this.globalPenaltiesLoaded = globalPenaltiesLoaded;
        }

        @Override
        public String toString() {
            return String.format("FlatPenaltyCache[professions=%d, entries=%d, global=%s]",
                    professionsLoaded, totalEntries, globalPenaltiesLoaded);
        }
    }

    private static final class Hit {
        final float value;
        final byte layer;

        Hit(float value, byte layer) {
            this.value = value;
            this.layer = layer;
        }
    }

    private static final class TypeTable {
        final Reference2ObjectMap<Block, Float> exact = new Reference2ObjectOpenHashMap<>();
        final List<TagRule> tags = new ArrayList<>();
        final byte exactLayer;
        final byte tagLayer;
        float defaultValue;

        TypeTable(float defaultValue, byte exactLayer, byte tagLayer) {
            this.defaultValue = defaultValue;
            this.exactLayer = exactLayer;
            this.tagLayer = tagLayer;
        }
    }

    private static final class TagRule {
        final TagKey<Block> tag;
        final TagKey<Item> itemTag;
        final float value;
        final String key;

        TagRule(ResourceLocation id, float value, String key) {
            this.tag = TagKey.create(Registries.BLOCK, id);
            this.itemTag = TagKey.create(Registries.ITEM, id);
            this.value = value;
            this.key = key;
        }

        boolean matches(Block block) {
            if (BuiltInRegistries.BLOCK.wrapAsHolder(block).is(tag)) return true;
            Item item = block.asItem();
            return item != Items.AIR && BuiltInRegistries.ITEM.wrapAsHolder(item).is(itemTag);
        }
    }

    private static final class Cell {
        final float walkOn;
        final float walkThrough;
        final byte walkOnLayer;
        final byte walkThroughLayer;

        Cell(float walkOn, byte walkOnLayer, float walkThrough, byte walkThroughLayer) {
            this.walkOn = walkOn;
            this.walkOnLayer = walkOnLayer;
            this.walkThrough = walkThrough;
            this.walkThroughLayer = walkThroughLayer;
        }
    }

    private static final class Table {
        static final Table EMPTY = new Table(new Reference2ObjectOpenHashMap<>(), CONSTANT_WALK_ON, CONSTANT_WALK_THROUGH);

        final Reference2ObjectMap<Block, Cell> cells;
        final float walkOnDefault;
        final float walkThroughDefault;

        Table(Reference2ObjectMap<Block, Cell> cells, float walkOnDefault, float walkThroughDefault) {
            this.cells = cells;
            this.walkOnDefault = walkOnDefault;
            this.walkThroughDefault = walkThroughDefault;
        }
    }

    private static final class Snapshot {
        final Map<String, Integer> index;
        final Table[] professions;
        final Table global;
        final List<List<ProcessedAvoidEntry>> avoids;
        final List<ProcessedAvoidEntry> globalAvoids;

        Snapshot(Map<String, Integer> index, Table[] professions, Table global,
                 List<List<ProcessedAvoidEntry>> avoids, List<ProcessedAvoidEntry> globalAvoids) {
            this.index = index;
            this.professions = professions;
            this.global = global;
            this.avoids = avoids;
            this.globalAvoids = globalAvoids;
        }
    }

    private static final class Diagnostics {
        private final Set<String> reported = new HashSet<>();
        private int tags;
        private int unresolved;
        private int emptyTags;

        void invalid(String owner, String type, String key) {
            unresolved++;
            report("Invalid key '" + key + "' in " + owner + "." + type + ", ignored");
        }

        void unknown(String owner, String type, String key) {
            unresolved++;
            report("Unknown block '" + key + "' in " + owner + "." + type + ", ignored");
        }

        void invalidAvoid(String owner, String key) {
            unresolved++;
            report("Invalid avoid block '" + key + "' in " + owner + ".avoids, ignored");
        }

        void emptyTag(String owner, String type, String key) {
            emptyTags++;
            report("Tag '" + key + "' in " + owner + "." + type + " matches no blocks");
        }

        void tag() {
            tags++;
        }

        private void report(String message) {
            if (reported.add(message)) LOGGER.warn(message);
        }
    }
}
