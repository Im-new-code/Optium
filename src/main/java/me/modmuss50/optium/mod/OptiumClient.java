package me.modmuss50.optium.mod;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import me.modmuss50.optium.gui.OptiumStatusScreen;

public final class OptiumClient implements ClientModInitializer {
    private static KeyBinding openStatusKey;

    @Override
    public void onInitializeClient() {
        openStatusKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.optium.open_status", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.optium"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openStatusKey.wasPressed()) {
                if (client.player != null || client.currentScreen instanceof TitleScreen) {
                    client.openScreen(new OptiumStatusScreen(client.currentScreen));
                }
            }
        });
    }
}
