package net.elevator.command;

import com.mojang.brigadier.CommandDispatcher;
import net.elevator.config.ElevatorConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ElevatorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("elevator")
                .then(Commands.literal("info")
                    .executes(context -> {
                        ElevatorConfig config = ElevatorConfig.get();
                        String blockId = config.elevatorBlock;
                        Block block = resolveBlock(blockId);

                        if (block == Blocks.AIR) {
                            context.getSource().sendSuccess(
                                () -> Component.literal("[Elevator] Configured elevator block is invalid: " + blockId),
                                false
                            );
                        } else {
                            context.getSource().sendSuccess(
                                () -> Component.literal("[Elevator] Place " + blockId + " on top of a redstone block to use it as an elevator."),
                                false
                            );
                        }
                        return 1;
                    })
                )
                .then(Commands.literal("reload")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .executes(context -> {
                        ElevatorConfig.load();
                        context.getSource().sendSuccess(
                            () -> Component.literal("[Elevator] Config reloaded!"),
                            true
                        );
                        return 1;
                    })
                )
        );
    }

    private static Block resolveBlock(String blockId) {
        try {
            Identifier identifier = Identifier.parse(blockId);
            return BuiltInRegistries.BLOCK.getValue(identifier);
        } catch (Exception ignored) {
            return Blocks.AIR;
        }
    }
}
