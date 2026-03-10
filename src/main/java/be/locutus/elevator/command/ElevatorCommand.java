package be.locutus.elevator.command;

import be.locutus.elevator.config.ElevatorConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ElevatorCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("elevator")
                .then(CommandManager.literal("reload")
                    .executes(context -> {
                        ElevatorConfig.load();
                        context.getSource().sendFeedback(
                            () -> Text.literal("[Elevator] Config reloaded!"),
                            true
                        );
                        return 1;
                    })
                )
        );
    }
}
