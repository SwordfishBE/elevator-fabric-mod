package net.elevator.command;

import com.mojang.brigadier.CommandDispatcher;
import net.elevator.config.ElevatorConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ElevatorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("elevator")
                .then(Commands.literal("reload")
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
}
