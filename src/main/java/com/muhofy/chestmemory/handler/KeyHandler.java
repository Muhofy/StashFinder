package com.muhofy.chestmemory.handler;

import com.muhofy.chestmemory.ChestMemoryMod;
import com.muhofy.chestmemory.ui.ChestRecordsScreen;
import com.muhofy.chestmemory.ui.SearchOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyHandler {

    public static KeyBinding searchKey;
    public static KeyBinding recordsKey;

    // 1.21.9+ — Category artık Identifier tabanlı
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(ChestMemoryMod.MOD_ID, "general"));

    public static void register() {
        searchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.stashfinder.search",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                CATEGORY
        ));

        recordsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.stashfinder.records",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) return;
            if (client.player == null) return;

            while (searchKey.wasPressed()) {
                openSearchOverlay(client);
            }

            while (recordsKey.wasPressed()) {
                openRecordsScreen(client);
            }
        });

        ChestMemoryMod.LOGGER.info("[KeyHandler] Keybinds registered.");
    }

    private static void openSearchOverlay(MinecraftClient client) {
        client.setScreen(new SearchOverlay());
    }

    private static void openRecordsScreen(MinecraftClient client) {
        client.setScreen(new ChestRecordsScreen());
    }
}