package net.elevator;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.TitleScreen;

public final class ClothConfigNoticeClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (ElevatorModMenuIntegration.hasCompatibleClothConfig()) {
                throw new AssertionError("This probe requires missing or pre-26.4 Cloth Config");
            }
            var screen = new ElevatorModMenuIntegration().getModConfigScreenFactory().create(new TitleScreen());
            if (!(screen instanceof AlertScreen)) throw new AssertionError("Expected Cloth Config notice");
            String narration = screen.getNarrationMessage().getString();
            if (!narration.contains("config/elevator.json") || !narration.contains("/elevator reload")) {
                throw new AssertionError("Notice must explain how to configure Elevator without the GUI");
            }
            if (FabricLoader.getInstance().isModLoaded("cloth-config") && !narration.contains("older version")) {
                throw new AssertionError("Old Cloth Config must be explained");
            }
            client.setScreenAndShow(screen);
        });
        context.waitTicks(5);
        context.setScreen(TitleScreen::new);
        ElevatorMod.LOGGER.info("CLOTH_NOTICE_TEST_PASS clothPresent={}",
            FabricLoader.getInstance().isModLoaded("cloth-config"));
    }
}
