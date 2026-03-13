package com.muhofy.chestmemory;

import com.muhofy.chestmemory.config.ChestMemoryConfig;
import com.muhofy.chestmemory.data.ChestStorage;
import com.muhofy.chestmemory.handler.ChestOpenHandler;
import com.muhofy.chestmemory.handler.KeyHandler;
import com.muhofy.chestmemory.handler.WorldEventHandler;
import com.muhofy.chestmemory.ui.ChestMemoryHud;
import com.muhofy.chestmemory.ui.IconManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChestMemoryMod implements ClientModInitializer {

    public static final String MOD_ID = "stashfinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[ChestMemory] Initializing...");

        ChestMemoryConfig.getInstance().load();
        ChestStorage.getInstance().init();
        KeyHandler.register();
        ChestOpenHandler.register();
        ChestMemoryHud.register();
        WorldEventHandler.register();

        // Oyun tamamen başlayınca ikon cache'ini sıfırla
        // böylece ResourceManager hazır olduğunda ikonlar doğru yüklenir
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            IconManager.get().invalidateCache();
            LOGGER.info("[ChestMemory] Icon cache invalidated after client start.");
        });

        LOGGER.info("[ChestMemory] Ready.");
    }
}