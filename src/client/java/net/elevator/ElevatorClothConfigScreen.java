package net.elevator;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.elevator.config.ElevatorConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ElevatorClothConfigScreen {
    private ElevatorClothConfigScreen() {
    }

    static Screen create(Screen parent) {
        ElevatorConfig config = ElevatorMod.loadConfigForEditing();
        String elevatorBlocksCsv = String.join(", ", config.elevatorBlocks);

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.literal("Elevator Config"))
            .setSavingRunnable(() -> ElevatorMod.applyEditedConfig(config));

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        ConfigEntryBuilder entries = builder.entryBuilder();

        general.addEntry(entries.startStrField(Component.literal("Elevator Blocks"), elevatorBlocksCsv)
            .setDefaultValue("minecraft:iron_block")
            .setTooltip(Component.literal("Comma-separated block IDs. Each listed block must sit on a redstone block, and travel only works between matching block types."))
            .setSaveConsumer(value -> config.elevatorBlocks = java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .distinct()
                .toList())
            .build());

        general.addEntry(entries.startIntField(Component.literal("Max Elevator Height"), config.maxElevatorHeight)
            .setDefaultValue(50)
            .setMin(1)
            .setTooltip(Component.literal("Maximum number of blocks to scan upward or downward."))
            .setSaveConsumer(value -> config.maxElevatorHeight = value)
            .build());

        general.addEntry(entries.startBooleanToggle(Component.literal("Particles Enabled"), config.particlesEnabled)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Spawn a configurable particle effect when teleporting."))
            .setSaveConsumer(value -> config.particlesEnabled = value)
            .build());

        general.addEntry(entries.startStrField(Component.literal("Particle Type"), config.particleType)
            .setDefaultValue("minecraft:portal")
            .setTooltip(Component.literal("Minecraft particle ID. Simple particles like minecraft:portal work best."))
            .setSaveConsumer(value -> config.particleType = value)
            .build());

        general.addEntry(entries.startBooleanToggle(Component.literal("Sound Enabled"), config.soundEnabled)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Play a configurable entity sound when teleporting."))
            .setSaveConsumer(value -> config.soundEnabled = value)
            .build());

        general.addEntry(entries.startStrField(Component.literal("Sound Event"), config.soundEvent)
            .setDefaultValue("minecraft:entity.enderman.teleport")
            .setTooltip(Component.literal("Minecraft sound event ID, ideally an entity sound like minecraft:entity.enderman.teleport."))
            .setSaveConsumer(value -> config.soundEvent = value)
            .build());

        general.addEntry(entries.startIntField(Component.literal("Cooldown Ticks"), config.cooldownTicks)
            .setDefaultValue(20)
            .setMin(0)
            .setTooltip(Component.literal("Cooldown between teleports in ticks. 20 ticks = 1 second."))
            .setSaveConsumer(value -> config.cooldownTicks = value)
            .build());

        general.addEntry(entries.startBooleanToggle(Component.literal("Safety Enabled"), config.safetyEnabled)
            .setDefaultValue(true)
            .setTooltip(Component.literal("Require enough headroom at the destination before teleporting."))
            .setSaveConsumer(value -> config.safetyEnabled = value)
            .build());

        return builder.build();
    }
}
