package com.muhofy.chestmemory.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.muhofy.chestmemory.ChestMemoryMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;

public class ChestMemoryConfig {

    // ── Singleton ─────────────────────────────────────────────────────────
    private static final ChestMemoryConfig INSTANCE = new ChestMemoryConfig();
    public static ChestMemoryConfig getInstance() { return INSTANCE; }
    private ChestMemoryConfig() {}

    // ── Enums ─────────────────────────────────────────────────────────────
    public enum ToastPosition  { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    public enum CompassPosition { TOP_LEFT, TOP_RIGHT }

    // ── Config alanları (default değerler) ───────────────────────────────
    public boolean         toastEnabled    = true;
    public ToastPosition   toastPosition   = ToastPosition.BOTTOM_RIGHT;
    public CompassPosition compassPosition = CompassPosition.TOP_LEFT;

    // ── I/O ───────────────────────────────────────────────────────────────
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Path getConfigPath() {
    return FabricLoader.getInstance().getConfigDir()
                       .resolve("stashfinder")
                       .resolve("config.json");
}

    public void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            save(); // default değerlerle oluştur
            return;
        }
        try (Reader r = Files.newBufferedReader(path)) {
            ChestMemoryConfig loaded = gson.fromJson(r, ChestMemoryConfig.class);
            if (loaded != null) {
                this.toastEnabled    = loaded.toastEnabled;
                this.toastPosition   = loaded.toastPosition   != null ? loaded.toastPosition   : ToastPosition.BOTTOM_RIGHT;
                this.compassPosition = loaded.compassPosition != null ? loaded.compassPosition : CompassPosition.TOP_LEFT;
            }
        } catch (Exception e) {
            ChestMemoryMod.LOGGER.error("[ChestMemoryConfig] Failed to load config, using defaults.", e);
            save();
        }
    }

    public void save() {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                gson.toJson(this, w);
            }
        } catch (IOException e) {
            ChestMemoryMod.LOGGER.error("[ChestMemoryConfig] Failed to save config.", e);
        }
    }
}