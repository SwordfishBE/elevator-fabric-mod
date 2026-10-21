package net.elevator.config;

import net.fabricmc.loader.impl.FabricLoaderImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class ElevatorConfigTest {
    @TempDir
    static Path configDirectory;

    @BeforeAll
    static void useTemporaryConfigDirectory() throws ReflectiveOperationException {
        // These unit tests run without launching Minecraft or initializing Fabric's game provider.
        var directory = FabricLoaderImpl.class.getDeclaredField("configDir");
        directory.setAccessible(true);
        directory.set(FabricLoaderImpl.INSTANCE, configDirectory);
    }

    @BeforeEach
    void resetConfig() throws Exception {
        Files.deleteIfExists(configDirectory.resolve("elevator.json"));
        ElevatorConfig.load();
    }

    @Test
    void migratesLegacyBlockWithoutAddingIron() throws Exception {
        Files.writeString(configDirectory.resolve("elevator.json"),
            "{\"elevatorBlock\":\"minecraft:diamond_block\"}");

        assertEquals(List.of("minecraft:diamond_block"), ElevatorConfig.load().elevatorBlocks);
        assertEquals(List.of("minecraft:diamond_block"), ElevatorConfig.load().elevatorBlocks);
        assertFalse(Files.readString(configDirectory.resolve("elevator.json")).contains("\"elevatorBlock\""));
    }

    @Test
    void modernBlockListTakesPrecedenceOverLegacyValue() {
        var config = ElevatorConfig.fromJson("""
            {"elevatorBlocks":["minecraft:gold_block"],"elevatorBlock":"minecraft:diamond_block"}
            """);
        assertEquals(List.of("minecraft:gold_block"), config.elevatorBlocks);
    }

    @Test
    void removingFirstBlockThroughEditorSurvivesSaveAndReload() {
        ElevatorConfig.applyEditedConfig(ElevatorConfig.fromJson("""
            {"elevatorBlocks":["minecraft:gold_block","minecraft:diamond_block"]}
            """));
        var draft = ElevatorConfig.loadForEditing();
        draft.elevatorBlocks = List.of("minecraft:diamond_block");
        ElevatorConfig.applyEditedConfig(draft);

        assertEquals(List.of("minecraft:diamond_block"), ElevatorConfig.get().elevatorBlocks);
        assertEquals(List.of("minecraft:diamond_block"), ElevatorConfig.load().elevatorBlocks);
    }

    @ParameterizedTest
    @ValueSource(strings = {"minecraft:ambient.cave", "minecraft:music.menu", "minecraft:weather.rain",
        "minecraft:block.bell.use", "minecraft:entity.enderman.teleport", "custom:machine.beep"})
    void preservesValidSoundIdentifiers(String identifier) {
        var draft = new ElevatorConfig();
        draft.soundEvent = identifier;
        assertEquals(identifier, draft.normalizedCopy().soundEvent);
    }

    @ParameterizedTest
    @ValueSource(strings = {"enderman.teleport", "minecraft:enderman.teleport"})
    void migratesKnownLegacyEndermanSound(String identifier) {
        var draft = new ElevatorConfig();
        draft.soundEvent = identifier;
        assertEquals("minecraft:entity.enderman.teleport", draft.normalizedCopy().soundEvent);
    }

    @Test
    void publishedConfigIsNormalizedAndDetachedFromMutableDraft() {
        var draft = new ElevatorConfig();
        var blocks = new ArrayList<>(List.of(" minecraft:gold_block ", "minecraft:gold_block"));
        draft.elevatorBlocks = blocks;
        draft.maxElevatorHeight = -1;
        draft.cooldownTicks = -20;
        draft.soundEvent = null;
        ElevatorConfig.applyEditedConfig(draft);
        blocks.clear();
        draft.maxElevatorHeight = 999;

        var published = ElevatorConfig.get();
        assertEquals(List.of("minecraft:gold_block"), published.elevatorBlocks);
        assertEquals(1, published.maxElevatorHeight);
        assertEquals(0, published.cooldownTicks);
        assertEquals("minecraft:entity.enderman.teleport", published.soundEvent);
        assertThrows(UnsupportedOperationException.class, () -> published.elevatorBlocks.clear());
    }

    @Test
    @Timeout(10)
    void guiEditsAreCapturedAndOnlyAppliedWhenServerExecutorRuns() throws Exception {
        var before = ElevatorConfig.get();
        String beforeFile = Files.readString(configDirectory.resolve("elevator.json"));
        var tasks = new ArrayDeque<Runnable>();
        var draft = new ElevatorConfig();
        draft.elevatorBlocks = new ArrayList<>(List.of(" minecraft:gold_block "));
        draft.cooldownTicks = -1;
        ElevatorConfig.applyEditedConfig(draft, tasks::add);

        assertSame(before, ElevatorConfig.get());
        assertEquals(beforeFile, Files.readString(configDirectory.resolve("elevator.json")));
        draft.elevatorBlocks.clear();
        draft.cooldownTicks = 123;

        try (var server = Executors.newSingleThreadExecutor()) {
            server.submit(tasks.remove()).get();
        }
        assertEquals(List.of("minecraft:gold_block"), ElevatorConfig.get().elevatorBlocks);
        assertEquals(0, ElevatorConfig.get().cooldownTicks);
        assertEquals(0, ElevatorConfig.load().cooldownTicks);
    }

    @Test
    @Timeout(10)
    void simultaneousReloadAndSaveCannotReadPartiallyWrittenConfig() throws Exception {
        var draft = new ElevatorConfig();
        draft.maxElevatorHeight = 123;
        ElevatorConfig.applyEditedConfig(draft);
        var barrier = new CyclicBarrier(2);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var writer = workers.submit(() -> {
                barrier.await();
                for (int i = 0; i < 40; i++) {
                    ElevatorConfig.applyEditedConfig(draft);
                }
                return null;
            });
            var reader = workers.submit(() -> {
                barrier.await();
                for (int i = 0; i < 40; i++) {
                    assertEquals(123, ElevatorConfig.load().maxElevatorHeight);
                    assertEquals(123, ElevatorConfig.get().maxElevatorHeight);
                }
                return null;
            });
            writer.get();
            reader.get();
        }
    }

    @Test
    void malformedConfigIsPreservedOnDisk() throws Exception {
        String broken = "{\"maxElevatorHeight\": broken}";
        Files.writeString(configDirectory.resolve("elevator.json"), broken);
        ElevatorConfig.load();
        assertEquals(broken, Files.readString(configDirectory.resolve("elevator.json")));
    }
}
