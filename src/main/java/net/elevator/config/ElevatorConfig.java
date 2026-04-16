package net.elevator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.elevator.ElevatorMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    /** Block IDs that may be used as elevator platforms. Each must be placed on a redstone block. */
    public List<String> elevatorBlocks = new ArrayList<>(List.of("minecraft:iron_block"));

    /** Legacy single-block config key, kept for migration from older config files. */
    public String elevatorBlock;

    /** Maximum search distance (in blocks) up or down. */
    public int maxElevatorHeight = 50;

    /** Show particle effects on teleport. */
    public boolean particlesEnabled = true;

    /** Particle ID spawned when teleporting. */
    public String particleType = "minecraft:portal";

    /** Play sound on teleport. */
    public boolean soundEnabled = true;

    /** Sound event ID played when teleporting. */
    public String soundEvent = "minecraft:entity.enderman.teleport";

    /** Cooldown in ticks between teleports (20 ticks = 1 second). */
    public int cooldownTicks = 20;

    /**
     * Safety check: only teleport if the destination has 2 free blocks above the elevator block.
     */
    public boolean safetyEnabled = true;

    public static ElevatorConfig load() {
        LoadResult loadResult = readOrCreateConfig();
        instance = loadResult.config();
        if (loadResult.shouldSave()) {
            instance.save();
        }
        return instance;
    }

    public static ElevatorConfig loadForEditing() {
        return readOrCreateConfig().config();
    }

    public static void applyEditedConfig(ElevatorConfig editedConfig) {
        instance = editedConfig == null ? new ElevatorConfig() : editedConfig;
        instance.normalize();
        instance.save();
    }

    private static LoadResult readOrCreateConfig() {
        File configFile = CONFIG_PATH.toFile();

        if (configFile.exists()) {
            try {
                String rawJson = Files.readString(CONFIG_PATH);
                ElevatorConfig loadedConfig = GSON.fromJson(stripComments(rawJson), ElevatorConfig.class);
                if (loadedConfig == null) {
                    loadedConfig = new ElevatorConfig();
                }
                loadedConfig.normalize();
                return new LoadResult(loadedConfig, true);
            } catch (Exception e) {
                ElevatorMod.LOGGER.warn(
                    "{} Failed to load config, using defaults without overwriting '{}': {}",
                    ElevatorMod.logPrefix(),
                    CONFIG_PATH.getFileName(),
                    e.getMessage()
                );
            }
        }

        ElevatorConfig config = new ElevatorConfig();
        config.normalize();
        return new LoadResult(config, !configFile.exists());
    }

    private void save() {
        try {
            Files.writeString(CONFIG_PATH, toCommentedJson());
        } catch (IOException e) {
            ElevatorMod.LOGGER.warn("{} Failed to save config: {}", ElevatorMod.logPrefix(), e.getMessage());
        }
    }

    private static String stripComments(String rawJson) {
        return COMMENT_PATTERN.matcher(rawJson).replaceAll("");
    }

    private void normalize() {
        List<String> normalizedBlocks = new ArrayList<>();
        if (elevatorBlocks != null) {
            for (String blockId : elevatorBlocks) {
                String normalized = normalizeIdentifier(blockId);
                if (normalized != null && !normalizedBlocks.contains(normalized)) {
                    normalizedBlocks.add(normalized);
                }
            }
        }

        String legacyBlock = normalizeIdentifier(elevatorBlock);
        if (legacyBlock != null && !normalizedBlocks.contains(legacyBlock)) {
            normalizedBlocks.add(legacyBlock);
        }

        if (normalizedBlocks.isEmpty()) {
            normalizedBlocks.add("minecraft:iron_block");
        }
        elevatorBlocks = normalizedBlocks;
        elevatorBlock = elevatorBlocks.getFirst();

        particleType = Objects.requireNonNullElse(normalizeIdentifier(particleType), "minecraft:portal");
        soundEvent = normalizeSoundIdentifier(soundEvent);

        if (maxElevatorHeight < 1) {
            maxElevatorHeight = 1;
        }
        if (cooldownTicks < 0) {
            cooldownTicks = 0;
        }
    }

    private static String normalizeIdentifier(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeSoundIdentifier(String value) {
        String normalized = normalizeIdentifier(value);
        if (normalized == null) {
            return "minecraft:entity.enderman.teleport";
        }

        if (normalized.equals("minecraft:enderman.teleport")) {
            return "minecraft:entity.enderman.teleport";
        }

        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }

        int separatorIndex = normalized.indexOf(':');
        String namespace = normalized.substring(0, separatorIndex);
        String path = normalized.substring(separatorIndex + 1);

        if (!path.startsWith("entity.") && path.chars().filter(character -> character == '.').count() == 1) {
            return namespace + ":entity." + path;
        }

        return normalized;
    }

    private String toCommentedJson() {
        String newline = System.lineSeparator();
        return "{" + newline
                + "  // Block IDs that may be used as elevator platforms. Every listed block must be placed on top of a redstone block." + newline
                + "  \"elevatorBlocks\": " + GSON.toJson(elevatorBlocks) + "," + newline
                + newline
                + "  // Maximum number of blocks to scan upward or downward for the next elevator platform." + newline
                + "  \"maxElevatorHeight\": " + maxElevatorHeight + "," + newline
                + newline
                + "  // Whether teleporting should spawn particles at the destination." + newline
                + "  \"particlesEnabled\": " + particlesEnabled + "," + newline
                + newline
                + "  // Particle ID to use when particles are enabled. Simple particles like minecraft:portal work best." + newline
                + "  \"particleType\": " + GSON.toJson(particleType) + "," + newline
                + newline
                + "  // Whether teleporting should play a sound effect." + newline
                + "  \"soundEnabled\": " + soundEnabled + "," + newline
                + newline
                + "  // Entity sound event ID to play when sound is enabled." + newline
                + "  \"soundEvent\": " + GSON.toJson(soundEvent) + "," + newline
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

    private record LoadResult(ElevatorConfig config, boolean shouldSave) {
    }
}
