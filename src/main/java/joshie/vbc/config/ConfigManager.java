package joshie.vbc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.RegistryAccess;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static joshie.vbc.VillagerBrainConfig.LOGGER;

public class ConfigManager {
    public static final Path CONFIG_PATH = Path.of("config/villager_brain_config.json");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static ModConfig CONFIG = new ModConfig();


    public static void loadConfig() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                CONFIG = GSON.fromJson(reader, ModConfig.class);

                if (CONFIG.professions == null) {
                    CONFIG.professions = new HashMap<>();
                }

            } catch (IOException | JsonSyntaxException e) {
                e.printStackTrace();
            }
        } else {
            generateDefaultConfig();
            saveConfig();
        }

        warnIgnoredProfessionDefaults();
    }

    public static void rebuild(RegistryAccess registryAccess) {
        if (CONFIG.professions == null) {
            CONFIG.professions = new HashMap<>();
        }
        FlatPenaltyCache.rebuild(registryAccess);
    }

    private static void warnIgnoredProfessionDefaults() {
        for (Map.Entry<String, ModConfig> entry : CONFIG.professions.entrySet()) {
            ModConfig config = entry.getValue();
            if (config == null) continue;
            if (config.walkOnBlockPenalties.containsKey("default") || config.walkThroughBlockPenalties.containsKey("default")) {
                LOGGER.warn("Profession '{}' declares default, ignored; use the top level default instead", entry.getKey());
            }
        }
    }

    public static void saveConfig() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(CONFIG, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static void generateDefaultConfig() {
        CONFIG.walkOnBlockPenalties.put("minecraft:lava", -1.0);
        CONFIG.walkOnBlockPenalties.put("minecraft:wither_rose", -1.0);
        CONFIG.walkOnBlockPenalties.put("minecraft:powder_snow", -1.0);
        CONFIG.walkOnBlockPenalties.put("minecraft:water", 64.0);
        CONFIG.walkOnBlockPenalties.put("minecraft:stone", 4.0);
        CONFIG.walkOnBlockPenalties.put("minecraft:cobblestone", 4.0);
        CONFIG.walkOnBlockPenalties.put("minecraft:farmland", 32.0);
        // Village Paths
        CONFIG.walkOnBlockPenalties.put("minecraft:gravel", 1.0);
        CONFIG.walkOnBlockPenalties.put("minecraft:dirt_path", 0.0);
        CONFIG.walkOnBlockPenalties.put("#minecraft:planks", 0.0);
        CONFIG.walkOnBlockPenalties.put("minecraft:coarse_dirt", 0.0);
        CONFIG.walkOnBlockPenalties.put("minecraft:packed_mud", 0.0);
        CONFIG.walkOnBlockPenalties.put("minecraft:mud_bricks", 0.0);
        CONFIG.walkOnBlockPenalties.put("#minecraft:stairs", 0.0);
        CONFIG.walkOnBlockPenalties.put("#minecraft:wool", 0.0);
        CONFIG.walkOnBlockPenalties.put("#material:granite", 0.0);
        // Village Blocks
        CONFIG.walkOnBlockPenalties.put("minecraft:grass_block", 5.0);
        CONFIG.walkOnBlockPenalties.put("#minecraft:leaves", 32.0);
        CONFIG.walkOnBlockPenalties.put("#minecraft:trapdoors", 64.0);
        CONFIG.walkOnBlockPenalties.put("#c:villager_job_sites", 32.0);
        CONFIG.walkOnBlockPenalties.put("default", 2.0);

        CONFIG.walkThroughBlockPenalties.put("minecraft:wither_rose", -1.0);
        CONFIG.walkThroughBlockPenalties.put("minecraft:cobweb", 20.0);
        CONFIG.walkThroughBlockPenalties.put("minecraft:water", 32.0);
        CONFIG.walkThroughBlockPenalties.put("minecraft:short_grass", 4.0);
        CONFIG.walkThroughBlockPenalties.put("#c:grass", 4.0);
        CONFIG.walkThroughBlockPenalties.put("minecraft:tall_grass", 8.0);
        CONFIG.walkThroughBlockPenalties.put("minecraft:fern", 4.0);
        CONFIG.walkThroughBlockPenalties.put("minecraft:large_fern", 8.0);
        CONFIG.walkThroughBlockPenalties.put("#minecraft:trapdoors", 32.0);
        CONFIG.walkThroughBlockPenalties.put("#minecraft:wool_carpets", 0.0);

        CONFIG.actionsPenalties.put("jump", 5.0);
        CONFIG.maxFallDistance = 1;

        CONFIG.avoids.add(new AvoidEntry("minecraft:lava", 3, -1.0));
        CONFIG.avoids.add(new AvoidEntry("minecraft:fire", 3, -1.0));
        CONFIG.avoids.add(new AvoidEntry("minecraft:soul_fire", 3, -1.0));
        CONFIG.avoids.add(new AvoidEntry("minecraft:magma_block", 2, -1.0));
        CONFIG.avoids.add(new AvoidEntry("minecraft:cactus", 2, -1.0));
        CONFIG.avoids.add(new AvoidEntry("minecraft:sweet_berry_bush", 2, -1.0));
        CONFIG.avoids.add(new AvoidEntry("minecraft:wither_rose", 3, -1.0));
        CONFIG.avoids.add(new AvoidEntry("minecraft:powder_snow", 2, -1.0));

        ModConfig armorerConfig = new ModConfig();
        armorerConfig.walkOnBlockPenalties.put("minecraft:stone", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:cobblestone", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:deepslate", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:andesite", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:diorite", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:granite", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:iron_block", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:anvil", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:chipped_anvil", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:damaged_anvil", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:blast_furnace", 0.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:smoker", 2.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:furnace", 2.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:farmland", 20.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:grass_block", 4.0);
        armorerConfig.walkOnBlockPenalties.put("minecraft:dirt_path", 2.0);
        armorerConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 12.0);
        armorerConfig.walkThroughBlockPenalties.put("minecraft:large_fern", 12.0);
        armorerConfig.avoids.add(new AvoidEntry("minecraft:water", 3, 16.0));
        CONFIG.professions.put("minecraft:armorer", armorerConfig);

        ModConfig butcherConfig = new ModConfig();
        butcherConfig.walkOnBlockPenalties.put("minecraft:grass_block", 2.0);
        butcherConfig.walkOnBlockPenalties.put("minecraft:dirt", 2.0);
        butcherConfig.walkOnBlockPenalties.put("minecraft:coarse_dirt", 2.0);
        butcherConfig.walkOnBlockPenalties.put("minecraft:hay_block", 0.0);
        butcherConfig.walkOnBlockPenalties.put("minecraft:smoker", 0.0);
        butcherConfig.walkOnBlockPenalties.put("minecraft:furnace", 2.0);
        butcherConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        butcherConfig.walkOnBlockPenalties.put("minecraft:farmland", 12.0);
        butcherConfig.walkOnBlockPenalties.put("minecraft:dirt_path", 0.0);
        butcherConfig.walkThroughBlockPenalties.put("minecraft:short_grass", 1.0);
        butcherConfig.walkThroughBlockPenalties.put("#c:grass", 1.0);
        butcherConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 2.0);
        butcherConfig.avoids.add(new AvoidEntry("minecraft:composter", 2, 8.0));
        CONFIG.professions.put("minecraft:butcher", butcherConfig);

        ModConfig cartographerConfig = new ModConfig();
        cartographerConfig.walkOnBlockPenalties.put("minecraft:sand", 2.0);
        cartographerConfig.walkOnBlockPenalties.put("minecraft:red_sand", 2.0);
        cartographerConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        cartographerConfig.walkOnBlockPenalties.put("minecraft:grass_block", 6.0);
        cartographerConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 10.0);
        cartographerConfig.walkThroughBlockPenalties.put("minecraft:large_fern", 10.0);
        cartographerConfig.avoids.add(new AvoidEntry("minecraft:lava", 1, -1.0));
        CONFIG.professions.put("minecraft:cartographer", cartographerConfig);

        ModConfig clericConfig = new ModConfig();
        clericConfig.walkOnBlockPenalties.put("minecraft:stone", 0.0);
        clericConfig.walkOnBlockPenalties.put("minecraft:cobblestone", 0.0);
        clericConfig.walkOnBlockPenalties.put("minecraft:stone_bricks", 0.0);
        clericConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        clericConfig.walkOnBlockPenalties.put("minecraft:gravel", 4.0);
        clericConfig.walkOnBlockPenalties.put("minecraft:farmland", 12.0);
        clericConfig.walkOnBlockPenalties.put("minecraft:dirt_path", 2.0);
        clericConfig.walkOnBlockPenalties.put("minecraft:grass_block", 8.0);
        clericConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 10.0);
        clericConfig.walkThroughBlockPenalties.put("minecraft:large_fern", 10.0);
        clericConfig.avoids.add(new AvoidEntry("minecraft:wither_rose", 5, -1.0));
        clericConfig.avoids.add(new AvoidEntry("minecraft:soul_fire", 4, -1.0));
        CONFIG.professions.put("minecraft:cleric", clericConfig);

        ModConfig farmerConfig = new ModConfig();
        farmerConfig.walkOnBlockPenalties.put("minecraft:farmland", 0.0);
        farmerConfig.walkOnBlockPenalties.put("minecraft:grass_block", 1.0);
        farmerConfig.walkOnBlockPenalties.put("minecraft:dirt", 1.0);
        farmerConfig.walkOnBlockPenalties.put("minecraft:hay_block", 0.0);
        farmerConfig.walkOnBlockPenalties.put("minecraft:water", 24.0);
        farmerConfig.walkOnBlockPenalties.put("minecraft:stone", 8.0);
        farmerConfig.walkOnBlockPenalties.put("minecraft:cobblestone", 8.0);
        farmerConfig.walkThroughBlockPenalties.put("minecraft:short_grass", 0.0);
        farmerConfig.walkThroughBlockPenalties.put("#c:grass", 0.0);
        farmerConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 1.0);
        farmerConfig.walkThroughBlockPenalties.put("minecraft:wheat", 2.0);
        farmerConfig.walkThroughBlockPenalties.put("minecraft:carrots", 2.0);
        farmerConfig.walkThroughBlockPenalties.put("minecraft:potatoes", 2.0);
        farmerConfig.walkThroughBlockPenalties.put("minecraft:beetroots", 2.0);
        CONFIG.professions.put("minecraft:farmer", farmerConfig);

        ModConfig fishermanConfig = new ModConfig();
        fishermanConfig.walkOnBlockPenalties.put("minecraft:water", 0.0);
        fishermanConfig.walkOnBlockPenalties.put("minecraft:sand", 2.0);
        fishermanConfig.walkOnBlockPenalties.put("minecraft:gravel", 1.0);
        fishermanConfig.walkOnBlockPenalties.put("minecraft:clay", 1.0);
        fishermanConfig.walkOnBlockPenalties.put("minecraft:prismarine", 0.0);
        fishermanConfig.walkOnBlockPenalties.put("minecraft:prismarine_bricks", 0.0);
        fishermanConfig.walkOnBlockPenalties.put("minecraft:dark_prismarine", 0.0);
        fishermanConfig.walkOnBlockPenalties.put("minecraft:sea_lantern", 0.0);
        fishermanConfig.walkOnBlockPenalties.put("minecraft:dirt_path", 2.0);
        fishermanConfig.walkOnBlockPenalties.put("minecraft:grass_block", 6.0);
        fishermanConfig.walkThroughBlockPenalties.put("minecraft:water", 0.0);
        fishermanConfig.walkThroughBlockPenalties.put("minecraft:kelp", 2.0);
        fishermanConfig.walkThroughBlockPenalties.put("minecraft:seagrass", 2.0);
        fishermanConfig.walkThroughBlockPenalties.put("minecraft:tall_seagrass", 4.0);
        CONFIG.professions.put("minecraft:fisherman", fishermanConfig);

        ModConfig fletcherConfig = new ModConfig();
        fletcherConfig.walkOnBlockPenalties.put("minecraft:grass_block", 2.0);
        fletcherConfig.walkOnBlockPenalties.put("minecraft:dirt", 2.0);
        fletcherConfig.walkOnBlockPenalties.put("minecraft:sand", 4.0);
        fletcherConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        fletcherConfig.walkOnBlockPenalties.put("minecraft:farmland", 16.0);
        fletcherConfig.walkThroughBlockPenalties.put("minecraft:short_grass", 1.0);
        fletcherConfig.walkThroughBlockPenalties.put("#c:grass", 1.0);
        fletcherConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 2.0);
        fletcherConfig.walkThroughBlockPenalties.put("minecraft:fern", 1.0);
        fletcherConfig.walkThroughBlockPenalties.put("minecraft:large_fern", 2.0);
        fletcherConfig.avoids.add(new AvoidEntry("minecraft:lava", 1, -1.0));
        CONFIG.professions.put("minecraft:fletcher", fletcherConfig);

        ModConfig leatherworkerConfig = new ModConfig();
        leatherworkerConfig.walkOnBlockPenalties.put("minecraft:grass_block", 2.0);
        leatherworkerConfig.walkOnBlockPenalties.put("minecraft:dirt", 2.0);
        leatherworkerConfig.walkOnBlockPenalties.put("minecraft:hay_block", 0.0);
        leatherworkerConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        leatherworkerConfig.walkOnBlockPenalties.put("minecraft:farmland", 12.0);
        leatherworkerConfig.walkThroughBlockPenalties.put("minecraft:short_grass", 1.0);
        leatherworkerConfig.walkThroughBlockPenalties.put("#c:grass", 1.0);
        leatherworkerConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 2.0);
        leatherworkerConfig.avoids.add(new AvoidEntry("minecraft:lava", 2, -1.0));
        CONFIG.professions.put("minecraft:leatherworker", leatherworkerConfig);

        ModConfig librarianConfig = new ModConfig();
        librarianConfig.walkOnBlockPenalties.put("minecraft:stone", 2.0);
        librarianConfig.walkOnBlockPenalties.put("minecraft:cobblestone", 2.0);
        librarianConfig.walkOnBlockPenalties.put("minecraft:stone_bricks", 0.0);
        librarianConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        librarianConfig.walkOnBlockPenalties.put("minecraft:farmland", 20.0);
        librarianConfig.walkOnBlockPenalties.put("minecraft:dirt_path", 2.0);
        librarianConfig.walkOnBlockPenalties.put("minecraft:grass_block", 10.0);
        librarianConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 12.0);
        librarianConfig.walkThroughBlockPenalties.put("minecraft:large_fern", 12.0);
        librarianConfig.avoids.add(new AvoidEntry("minecraft:water", 3, 20.0));
        librarianConfig.avoids.add(new AvoidEntry("minecraft:lava", 5, -1.0));
        CONFIG.professions.put("minecraft:librarian", librarianConfig);

        ModConfig masonConfig = new ModConfig();
        masonConfig.walkOnBlockPenalties.put("minecraft:stone", 0.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:cobblestone", 0.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:stone_bricks", 0.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:smooth_stone", 0.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:andesite", 0.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:diorite", 0.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:granite", 0.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:deepslate", 0.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:blackstone", 0.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:farmland", 20.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:dirt_path", 4.0);
        masonConfig.walkOnBlockPenalties.put("minecraft:grass_block", 10.0);
        masonConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 12.0);
        masonConfig.walkThroughBlockPenalties.put("minecraft:large_fern", 12.0);
        masonConfig.avoids.add(new AvoidEntry("minecraft:water", 3, 16.0));
        CONFIG.professions.put("minecraft:mason", masonConfig);

        ModConfig shepherdConfig = new ModConfig();
        shepherdConfig.walkOnBlockPenalties.put("minecraft:grass_block", 0.0);
        shepherdConfig.walkOnBlockPenalties.put("minecraft:dirt", 1.0);
        shepherdConfig.walkOnBlockPenalties.put("minecraft:hay_block", 0.0);
        shepherdConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        shepherdConfig.walkOnBlockPenalties.put("minecraft:farmland", 12.0);
        shepherdConfig.walkOnBlockPenalties.put("minecraft:stone", 8.0);
        shepherdConfig.walkOnBlockPenalties.put("minecraft:cobblestone", 8.0);
        shepherdConfig.walkThroughBlockPenalties.put("minecraft:short_grass", 0.0);
        shepherdConfig.walkThroughBlockPenalties.put("#c:grass", 0.0);
        shepherdConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 1.0);
        shepherdConfig.walkThroughBlockPenalties.put("minecraft:fern", 0.0);
        shepherdConfig.walkThroughBlockPenalties.put("minecraft:large_fern", 1.0);
        shepherdConfig.avoids.add(new AvoidEntry("minecraft:lava", 4, -1.0));
        CONFIG.professions.put("minecraft:shepherd", shepherdConfig);

        ModConfig toolsmithConfig = new ModConfig();
        toolsmithConfig.walkOnBlockPenalties.put("minecraft:stone", 0.0);
        toolsmithConfig.walkOnBlockPenalties.put("minecraft:cobblestone", 0.0);
        toolsmithConfig.walkOnBlockPenalties.put("minecraft:iron_block", 0.0);
        toolsmithConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        toolsmithConfig.walkOnBlockPenalties.put("minecraft:farmland", 20.0);
        toolsmithConfig.walkOnBlockPenalties.put("minecraft:dirt_path", 2.0);
        toolsmithConfig.walkOnBlockPenalties.put("minecraft:grass_block", 8.0);
        toolsmithConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 10.0);
        toolsmithConfig.walkThroughBlockPenalties.put("minecraft:large_fern", 10.0);
        toolsmithConfig.avoids.add(new AvoidEntry("minecraft:water", 3, 16.0));
        CONFIG.professions.put("minecraft:toolsmith", toolsmithConfig);

        ModConfig weaponsmithConfig = new ModConfig();
        weaponsmithConfig.walkOnBlockPenalties.put("minecraft:stone", 1.0);
        weaponsmithConfig.walkOnBlockPenalties.put("minecraft:cobblestone", 1.0);
        weaponsmithConfig.walkOnBlockPenalties.put("minecraft:iron_block", 0.0);
        weaponsmithConfig.walkOnBlockPenalties.put("minecraft:water", -1.0);
        weaponsmithConfig.walkOnBlockPenalties.put("minecraft:farmland", 20.0);
        weaponsmithConfig.walkOnBlockPenalties.put("minecraft:dirt_path", 2.0);
        weaponsmithConfig.walkOnBlockPenalties.put("minecraft:grass_block", 8.0);
        weaponsmithConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 10.0);
        weaponsmithConfig.walkThroughBlockPenalties.put("minecraft:large_fern", 10.0);
        weaponsmithConfig.avoids.add(new AvoidEntry("minecraft:water", 3, 16.0));
        CONFIG.professions.put("minecraft:weaponsmith", weaponsmithConfig);

        ModConfig nitwitConfig = new ModConfig();
        nitwitConfig.walkOnBlockPenalties.put("minecraft:gravel", 4.0);
        nitwitConfig.walkOnBlockPenalties.put("minecraft:sand", 4.0);
        nitwitConfig.walkOnBlockPenalties.put("minecraft:dirt", 2.0);
        nitwitConfig.walkOnBlockPenalties.put("minecraft:grass_block", 2.0);
        nitwitConfig.walkOnBlockPenalties.put("minecraft:water", 8.0);
        nitwitConfig.walkOnBlockPenalties.put("minecraft:farmland", 8.0);
        nitwitConfig.walkOnBlockPenalties.put("minecraft:dirt_path", 2.0);
        nitwitConfig.walkThroughBlockPenalties.put("minecraft:water", 0.0);
        nitwitConfig.walkThroughBlockPenalties.put("minecraft:short_grass", 1.0);
        nitwitConfig.walkThroughBlockPenalties.put("#c:grass", 1.0);
        nitwitConfig.walkThroughBlockPenalties.put("minecraft:tall_grass", 1.0);
        nitwitConfig.walkThroughBlockPenalties.put("minecraft:fern", 1.0);
        nitwitConfig.walkThroughBlockPenalties.put("minecraft:large_fern", 1.0);
        CONFIG.professions.put("minecraft:nitwit", nitwitConfig);
    }

    public static Float getActionPenalty(String professionId, String actionKey) {
        ModConfig p = CONFIG.professions.get(professionId);
        if (p != null && p.actionsPenalties.containsKey(actionKey)) {
            return p.actionsPenalties.get(actionKey).floatValue() + CONFIG.actionsPenalties.getOrDefault(actionKey, 0.0).floatValue();
        }
        return CONFIG.actionsPenalties.getOrDefault(actionKey, 0.0).floatValue();
    }

    public static int getMaxFallDistance() {
        return Math.max(0, CONFIG.maxFallDistance);
    }

}
