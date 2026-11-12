package dev.blockbrawlers.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class BlockBrawlersClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blockbrawlers.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.blockbrawlers"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new BlockBrawlersScreen());
                }
            }
        });
    }
}
