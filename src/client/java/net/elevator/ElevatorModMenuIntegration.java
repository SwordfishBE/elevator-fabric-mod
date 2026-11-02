package net.elevator;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.network.chat.Component;

public final class ElevatorModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return parent -> new AlertScreen(() -> Minecraft.getInstance().setScreenAndShow(parent),
                Component.literal("Elevator"),
                Component.literal("Install Cloth Config for Minecraft 26.4 to edit the configuration here. "
                    + "You can also edit config/elevator.json and use /elevator reload."));
        }
        if (!hasCompatibleClothConfig()) {
            return parent -> new AlertScreen(() -> Minecraft.getInstance().setScreenAndShow(parent),
                Component.literal("Elevator"),
                Component.literal("The configuration menu requires Cloth Config for Minecraft 26.4. "
                    + "The installed older version cannot render this screen safely. "
                    + "Until a compatible version is available, edit config/elevator.json and use /elevator reload."));
        }

        return ElevatorClothConfigScreen::create;
    }

    static boolean hasCompatibleClothConfig() {
        var cloth = FabricLoader.getInstance().getModContainer("cloth-config");
        if (cloth.isEmpty()) return false;
        try {
            return VersionPredicate.parse(">=26.4-").test(cloth.get().getMetadata().getVersion());
        } catch (VersionParsingException exception) {
            return false;
        }
    }
}
