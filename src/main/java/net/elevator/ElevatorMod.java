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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElevatorMod implements ModInitializer {

    public static final String MOD_ID = "elevator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ElevatorConfig config = ElevatorConfig.load();
        LOGGER.info("[Elevator] Mod loaded!");
        LOGGER.info("[Elevator]   Elevator block: {}", config.elevatorBlock);
        LOGGER.info("[Elevator]   Max height:     {}", config.maxElevatorHeight);

        ElevatorHandler handler = new ElevatorHandler();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            ElevatorCommand.register(dispatcher)
        );

        ServerTickEvents.END_SERVER_TICK.register(handler::onServerTick);

        ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, server) ->
            handler.removePlayer(networkHandler.player.getUUID())
        );

        ServerLifecycleEvents.SERVER_STARTED.register(server -> ModrinthUpdateChecker.checkOnceAsync());

        LOGGER.info("[Elevator] Ready!");
    }
}
