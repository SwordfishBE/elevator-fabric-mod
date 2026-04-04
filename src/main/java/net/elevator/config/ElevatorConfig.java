package net.elevator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.elevator.ElevatorMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Configuration for the Elevator mod.
 * Stored as JSON at: config/elevator.json
 */
public class ElevatorConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?m)^\\s*//.*(?:\\R|$)");
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("elevator.json");
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
        instance = readOrCreateConfig();
        instance.save();
        return instance;
    }

    public static ElevatorConfig loadForEditing() {
        return readOrCreateConfig();
    }

    public static void applyEditedConfig(ElevatorConfig editedConfig) {
        instance = editedConfig == null ? new ElevatorConfig() : editedConfig;
        instance.save();
    }

    private static ElevatorConfig readOrCreateConfig() {
        File configFile = CONFIG_PATH.toFile();

        if (configFile.exists()) {
            try {
                String rawJson = Files.readString(CONFIG_PATH);
                ElevatorConfig loadedConfig = GSON.fromJson(stripComments(rawJson), ElevatorConfig.class);
                if (loadedConfig == null) {
                    loadedConfig = new ElevatorConfig();
                }
                return loadedConfig;
            } catch (IOException e) {
                ElevatorMod.LOGGER.warn("[Elevator] Failed to load config, using defaults: {}", e.getMessage());
            }
        }

        return new ElevatorConfig();
    }

    private void save() {
        try {
            Files.writeString(CONFIG_PATH, toCommentedJson());
        } catch (IOException e) {
            ElevatorMod.LOGGER.warn("[Elevator] Failed to save config: {}", e.getMessage());
        }
    }

    private static String stripComments(String rawJson) {
        return COMMENT_PATTERN.matcher(rawJson).replaceAll("");
    }

    private String toCommentedJson() {
        String newline = System.lineSeparator();
        return "{" + newline
                + "  // Block ID used as the elevator platform. It must be placed on top of a redstone block." + newline
                + "  \"elevatorBlock\": \"" + elevatorBlock + "\"," + newline
                + newline
                + "  // Maximum number of blocks to scan upward or downward for the next elevator platform." + newline
                + "  \"maxElevatorHeight\": " + maxElevatorHeight + "," + newline
                + newline
                + "  // Whether teleporting should spawn portal particles at the origin and destination." + newline
                + "  \"particlesEnabled\": " + particlesEnabled + "," + newline
                + newline
                + "  // Whether teleporting should play the enderman teleport sound effect." + newline
                + "  \"soundEnabled\": " + soundEnabled + "," + newline
                + newline
                + "  // Cooldown between teleports in ticks. 20 ticks equals 1 second." + newline
                + "  \"cooldownTicks\": " + cooldownTicks + "," + newline
                + newline
                + "  // When enabled, the destination must have enough headroom for the player to arrive safely." + newline
                + "  \"safetyEnabled\": " + safetyEnabled + newline
                + "}" + newline;
    }

    public static ElevatorConfig get() {
        return instance;
    }
}
