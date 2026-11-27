package joshie.vbc;

import joshie.vbc.config.ConfigManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import joshie.vbc.commands.MoveVillagerCommand;
import static joshie.vbc.config.ConfigManager.loadConfig;


@Mod("villager_brain_config")
public class VillagerBrainConfig {
    public static final String MODNAME = "Villager Brain Config";
    public static final String ID = "villager_brain_config";
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);

    public VillagerBrainConfig(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);
    }


    public void onInitialize() {
        loadConfig();
    }


    public void commonSetup(FMLCommonSetupEvent event) { onInitialize(); }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            joshie.vbc.config.ProfessionCache.cleanupDeadReferences();
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ConfigManager.rebuild(event.getServer().registryAccess());
    }

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) {
            ConfigManager.rebuild(event.getRegistryAccess());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MoveVillagerCommand.register(event.getDispatcher());
        joshie.vbc.commands.PerformanceCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        joshie.vbc.config.ProfessionCache.clear();
        joshie.vbc.config.TickLocalCache.clear();
        joshie.vbc.config.VillagerGlobalTickCache.reset();
        joshie.vbc.utils.BlockPosPool.clear();
    }
}
