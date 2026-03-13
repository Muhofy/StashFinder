package com.muhofy.chestmemory.handler;

import com.muhofy.chestmemory.ChestMemoryMod;
import com.muhofy.chestmemory.data.ChestStorage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;

public class WorldEventHandler {

    public static void register() {

        // Dünyaya bağlanınca storage'ı yükle
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String worldName = resolveWorldName(client);
            ChestMemoryMod.LOGGER.info("[WorldEventHandler] Joined world: {}", worldName);
            ChestStorage.getInstance().loadWorld(worldName);
        });

        // Dünyadan çıkınca storage'ı temizle
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ChestMemoryMod.LOGGER.info("[WorldEventHandler] Left world, unloading storage.");
            ChestStorage.getInstance().unloadWorld();
        });

        ChestMemoryMod.LOGGER.info("[WorldEventHandler] Registered.");
    }

    /**
     * Singleplayer'da dünya klasör adını, multiplayer'da server adresini kullan.
     */
    private static String resolveWorldName(MinecraftClient client) {
        // Singleplayer
        if (client.isInSingleplayer() && client.getServer() != null) {
            return client.getServer().getSaveProperties().getLevelName();
        }
        // Multiplayer
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            return "server_" + serverInfo.address.replace(":", "_");
        }
        return "unknown_world";
    }
}