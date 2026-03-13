package com.muhofy.chestmemory.ui;

import com.muhofy.chestmemory.ChestMemoryMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class IconManager {

    // ── Singleton ─────────────────────────────────────────────────────────
    private static final IconManager INSTANCE = new IconManager();
    public static IconManager get() { return INSTANCE; }
    private IconManager() {}

    // ── Registry ──────────────────────────────────────────────────────────
    private static final Map<String, IconDef> ICONS = new HashMap<>();

    static {
        reg("chest",         "📦");
        reg("chest_double",  "🗄");
        reg("navigate",      "📍");
        reg("delete",        "🗑");
        reg("rename",        "✏");
        reg("dim_overworld", "🌿");
        reg("dim_nether",    "🔥");
        reg("dim_end",       "⭐");
        reg("search",        "🔍");
        reg("arrived",       "✅");
    }

    private static void reg(String name, String fallback) {
        // Identifier sadece namespace:path formatında olmalı, assets/ prefix YOK
        Identifier id = Identifier.of(ChestMemoryMod.MOD_ID,
                "textures/gui/icons/" + name + ".png");
        ICONS.put(name, new IconDef(id, fallback));
    }

    // ── Cache: null=bilinmiyor, true=PNG var, false=PNG yok ──────────────
    private final Map<String, Boolean> cache = new HashMap<>();

    public void invalidateCache() { cache.clear(); }

    // ── API ───────────────────────────────────────────────────────────────

    public boolean hasPng(String name) {
        if (!ICONS.containsKey(name)) return false;
        if (cache.containsKey(name))  return cache.get(name);

        // İlk erişimde ResourceManager ile kontrol et
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getResourceManager() == null) return false;
            Identifier id  = ICONS.get(name).id;
            boolean exists = mc.getResourceManager().getResource(id).isPresent();
            cache.put(name, exists);
            return exists;
        } catch (Exception e) {
            cache.put(name, false);
            return false;
        }
    }

    /**
     * PNG varsa çizer, yoksa unicode fallback gösterir.
     * Mor-siyah kare çıkmasın diye drawTexture exception'ı yakalanır.
     */
    public void draw(DrawContext ctx, String name, int x, int y) {
        IconDef def = ICONS.get(name);
        if (def == null) return;

        if (hasPng(name)) {
            try {
                ctx.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        def.id,
                        x, y,
                        0f, 0f,
                        16, 16,
                        16, 16
                );
                return;
            } catch (Exception e) {
                // Texture yüklenemedi — cache'i temizle, fallback'e düş
                ChestMemoryMod.LOGGER.warn("[IconManager] drawTexture failed for '{}', using fallback.", name);
                cache.put(name, false);
            }
        }

        // Unicode fallback
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.textRenderer != null) {
            ctx.drawTextWithShadow(mc.textRenderer,
                    Text.literal(def.fallback), x, y + 2, 0xFFFFFFFF);
        }
    }

    public String fallback(String name) {
        IconDef def = ICONS.get(name);
        return def != null ? def.fallback : "?";
    }

    // ── Record ────────────────────────────────────────────────────────────
    private record IconDef(Identifier id, String fallback) {}
}