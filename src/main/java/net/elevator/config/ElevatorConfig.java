package net.elevator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.elevator.ElevatorMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

/**
 * Configuration for the Elevator mod.
 * Stored as JSON at: config/elevator.json
 */
public class ElevatorConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?m)^\\s*//.*(?:\\R|$)");
    private static volatile ElevatorConfig instance;

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("elevator.json");
    }

    /** Block IDs that may be used as elevator platforms. Each must be placed on a redstone block. */
    public List<String> elevatorBlocks = new ArrayList<>(List.of("minecraft:iron_block"));

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

    public static synchronized ElevatorConfig load() {
        LoadResult loadResult = readOrCreateConfig();
        ElevatorConfig loadedConfig = loadResult.config();
        if (loadResult.shouldSave()) {
            loadedConfig.save();
        }
        instance = loadedConfig;
        return loadedConfig;
    }

    public static synchronized ElevatorConfig loadForEditing() {
        return readOrCreateConfig().config();
    }

    public static synchronized void applyEditedConfig(ElevatorConfig editedConfig) {
        ElevatorConfig snapshot = (editedConfig == null ? new ElevatorConfig() : editedConfig).normalizedCopy();
        snapshot.save();
        instance = snapshot;
    }

    public static void applyEditedConfig(ElevatorConfig editedConfig, Executor executor) {
        // Detach the GUI draft before it crosses to the server thread.
        ElevatorConfig snapshot = (editedConfig == null ? new ElevatorConfig() : editedConfig).normalizedCopy();
        executor.execute(() -> applyEditedConfig(snapshot));
    }

    private static LoadResult readOrCreateConfig() {
        Path configPath = configPath();
        File configFile = configPath.toFile();

        if (configFile.exists()) {
            try {
                String rawJson = Files.readString(configPath);
                ElevatorConfig loadedConfig = fromJson(rawJson);
                return new LoadResult(loadedConfig, true);
            } catch (Exception e) {
                ElevatorMod.LOGGER.warn(
                    "{} Failed to load config, using defaults without overwriting '{}': {}",
                    ElevatorMod.logPrefix(),
                    configPath.getFileName(),
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
            Files.writeString(configPath(), toCommentedJson());
        } catch (IOException e) {
            ElevatorMod.LOGGER.warn("{} Failed to save config: {}", ElevatorMod.logPrefix(), e.getMessage());
        }
    }

    private static String stripComments(String rawJson) {
        return COMMENT_PATTERN.matcher(rawJson).replaceAll("");
    }

    static ElevatorConfig fromJson(String rawJson) {
        JsonElement root = JsonParser.parseString(stripComments(rawJson));
        if (root.isJsonNull()) {
            return new ElevatorConfig().normalizedCopy();
        }
        JsonObject json = root.getAsJsonObject();
        ElevatorConfig config = GSON.fromJson(json, ElevatorConfig.class);
        if (!json.has("elevatorBlocks") && json.has("elevatorBlock")) {
            String legacyBlock = GSON.fromJson(json.get("elevatorBlock"), String.class);
            config.elevatorBlocks = legacyBlock == null ? List.of() : List.of(legacyBlock);
        }
        config.normalize();
        return config;
    }

    ElevatorConfig normalizedCopy() {
        ElevatorConfig copy = new ElevatorConfig();
        copy.elevatorBlocks = elevatorBlocks == null ? null : new ArrayList<>(elevatorBlocks);
        copy.maxElevatorHeight = maxElevatorHeight;
        copy.particlesEnabled = particlesEnabled;
        copy.particleType = particleType;
        copy.soundEnabled = soundEnabled;
        copy.soundEvent = soundEvent;
        copy.cooldownTicks = cooldownTicks;
        copy.safetyEnabled = safetyEnabled;
        copy.normalize();
        return copy;
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

        if (normalizedBlocks.isEmpty()) {
            normalizedBlocks.add("minecraft:iron_block");
        }
        elevatorBlocks = List.copyOf(normalizedBlocks);

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

        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }

        if (normalized.equals("minecraft:enderman.teleport")) {
            return "minecraft:entity.enderman.teleport";
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
