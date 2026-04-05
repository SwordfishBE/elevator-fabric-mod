package net.elevator;

import net.elevator.command.ElevatorCommand;
import net.elevator.config.ElevatorConfig;
import net.elevator.handler.ElevatorHandler;
import net.elevator.util.ModrinthUpdateChecker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElevatorMod implements ModInitializer {

    public static final String MOD_ID = "elevator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String FALLBACK_MOD_NAME = "Elevator";

    public static ElevatorConfig loadConfigForEditing() {
        return ElevatorConfig.loadForEditing();
    }

    public static void applyEditedConfig(ElevatorConfig editedConfig) {
        ElevatorConfig.applyEditedConfig(editedConfig);
    }

    @Override
    public void onInitialize() {
        ElevatorConfig config = ElevatorConfig.load();
        LOGGER.info("{} Mod initialized. Version: {}", logPrefix(), modVersion());
        LOGGER.debug("{} Elevator block: {}", logPrefix(), config.elevatorBlock);
        LOGGER.debug("{} Max height: {}", logPrefix(), config.maxElevatorHeight);

        ElevatorHandler handler = new ElevatorHandler();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            ElevatorCommand.register(dispatcher)
        );

        ServerTickEvents.END_SERVER_TICK.register(handler::onServerTick);

        ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, server) ->
            handler.removePlayer(networkHandler.player.getUUID())
        );

        ServerLifecycleEvents.SERVER_STARTED.register(server -> ModrinthUpdateChecker.checkOnceAsync());
        LOGGER.debug("{} Mod ready.", logPrefix());
    }

    public static String modName() {
        return modMetadata().map(ModMetadata::getName).orElse(FALLBACK_MOD_NAME);
    }

    public static String modVersion() {
        return modMetadata()
            .map(metadata -> metadata.getVersion().getFriendlyString())
            .orElse("unknown");
    }

    public static String logPrefix() {
        return "[" + modName() + "]";
    }

    private static java.util.Optional<ModMetadata> modMetadata() {
        return FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata());
    }
}
