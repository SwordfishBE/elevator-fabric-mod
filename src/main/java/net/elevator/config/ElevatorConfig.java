package net.elevator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

/**
 * Configuration for the Elevator mod.
 * Stored as JSON at: config/elevator.json
 */
public class ElevatorConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ElevatorConfig instance;

    /** Block ID used as elevator. Must be placed on a redstone block to activate. */
    public String elevatorBlock = "minecraft:iron_block";

    /** Maximum search distance (in blocks) up or down. */
    public int maxElevatorHeight = 50;

    /** Show particle effects on teleport. */
    public boolean particlesEnabled = true;

    /** Play sound on teleport. */
    public boolean soundEnabled = true;

    /** Cooldown in ticks between teleports (20 ticks = 1 second). */
    public int cooldownTicks = 20;

    /**
     * Safety check: only teleport if the destination has 2 free blocks above the elevator block.
     */
    public boolean safetyEnabled = true;

    public static ElevatorConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("elevator.json");
        File configFile = configPath.toFile();

        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                instance = GSON.fromJson(reader, ElevatorConfig.class);
                instance.save(configPath);
                return instance;
            } catch (IOException e) {
                System.err.println("[Elevator] Failed to load config, using defaults: " + e.getMessage());
            }
        }

        instance = new ElevatorConfig();
        instance.save(configPath);
        return instance;
    }

    private void save(Path path) {
        try (Writer writer = new FileWriter(path.toFile())) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("[Elevator] Failed to save config: " + e.getMessage());
        }
    }

    public static ElevatorConfig get() {
        return instance;
    }
}
