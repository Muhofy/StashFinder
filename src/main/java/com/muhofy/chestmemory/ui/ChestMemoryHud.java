package com.muhofy.chestmemory.ui;

import com.muhofy.chestmemory.ChestMemoryMod;
import com.muhofy.chestmemory.config.ChestMemoryConfig;
import com.muhofy.chestmemory.data.ChestRecord;
import com.muhofy.chestmemory.data.ChestStorage;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;

public class ChestMemoryHud {

    public enum ToastType { SUCCESS, INFO }

    private record Toast(String titleKey, String subtitle, ToastType type,
                         long createdAt, long duration, int count) {
        float alpha(long now) {
            long e = now - createdAt;
            if (e < 400)            return e / 400f;
            if (e > duration - 500) return Math.max(0f, (duration - e) / 500f);
            return 1f;
        }
        Toast withCount(int n) {
            return new Toast(titleKey, subtitle, type, System.currentTimeMillis(), duration, n);
        }
    }

    private static final Deque<Toast> toasts     = new ArrayDeque<>();
    private static final int          MAX_TOASTS = 3;
    private static ChestRecord        activeTarget = null;

    // ── Strip sabitleri ───────────────────────────────────────────────────
    private static final int STRIP_W     = 200;
    private static final int STRIP_H     = 18;
    private static final int INFO_H      = 14;
    private static final int TICK_STEP   = 10; // derece başına px
    private static final float PX_PER_DEG = TICK_STEP / 10f;

    // ── Renkler ───────────────────────────────────────────────────────────
    private static final int C_BG         = 0xCC000000;
    private static final int C_BORDER     = 0xFF333333;
    private static final int C_TICK_MIN   = 0x66FFFFFF;
    private static final int C_TICK_MAJ   = 0xCCFFFFFF;
    private static final int C_NORTH      = 0xFFFF5555;
    private static final int C_TARGET     = 0xFF55FFFF;
    private static final int C_TARGET_DIM = 0x8855FFFF;
    private static final int C_CENTER     = 0xFFFFFF55;
    private static final int C_NAME       = 0xFFEEEEEE;
    private static final int C_DIST       = 0xFFFFFF55;
    private static final int C_INFO_BG    = 0xDD000000;

    // ── Register ──────────────────────────────────────────────────────────
    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            boolean chestOpen = mc.currentScreen instanceof
                    net.minecraft.client.gui.screen.ingame.GenericContainerScreen;

            long now = System.currentTimeMillis();
            toasts.removeIf(t -> now - t.createdAt() >= t.duration());

            if (chestOpen) renderToasts(ctx, mc, now);
            else           toasts.clear();

            if (activeTarget != null) renderStrip(ctx, mc);
        });
        ChestMemoryMod.LOGGER.info("[ChestMemoryHud] Registered.");
    }

    // ── API ───────────────────────────────────────────────────────────────
    public static void pushToast(String titleKey, String subtitle, ToastType type) {
        if (!ChestMemoryConfig.getInstance().toastEnabled) return;
        if (subtitle != null && subtitle.contains("0 item")) return;
        for (Toast existing : toasts) {
            if (existing.titleKey().equals(titleKey)) {
                toasts.remove(existing);
                toasts.addLast(existing.withCount(existing.count() + 1));
                return;
            }
        }
        toasts.addLast(new Toast(titleKey, subtitle, type, System.currentTimeMillis(), 2500L, 1));
        while (toasts.size() > MAX_TOASTS) toasts.pollFirst();
    }

    public static void setTarget(ChestRecord rec) { activeTarget = rec; }
    public static ChestRecord getTarget()         { return activeTarget; }
    public static void clearTarget()              { activeTarget = null; }

    // ── Strip ─────────────────────────────────────────────────────────────
    private static void renderStrip(DrawContext ctx, MinecraftClient mc) {
        double px   = mc.player.getX();
        double pz   = mc.player.getZ();
        double dist = activeTarget.distanceTo(px, pz);

        if (dist <= 5) {
            pushToast("stashfinder.toast.arrived",
                    ChestStorage.getInstance().getDisplayName(activeTarget),
                    ToastType.SUCCESS);
            activeTarget = null;
            return;
        }

        int sw      = mc.getWindow().getScaledWidth();
        int stripX  = (sw - STRIP_W) / 2;
        int stripY  = 6;
        int centerX = stripX + STRIP_W / 2;

        // ── Arka plan ─────────────────────────────────────────────────────
        ctx.fill(stripX, stripY, stripX + STRIP_W, stripY + STRIP_H, C_BG);
        // Üst/alt border
        ctx.fill(stripX, stripY,              stripX + STRIP_W, stripY + 1,          0xFF444444);
        ctx.fill(stripX, stripY + STRIP_H - 1, stripX + STRIP_W, stripY + STRIP_H,   0xFF444444);
        // Sol/sağ border
        ctx.fill(stripX,              stripY, stripX + 1,          stripY + STRIP_H, 0xFF444444);
        ctx.fill(stripX + STRIP_W - 1, stripY, stripX + STRIP_W,   stripY + STRIP_H, 0xFF444444);

        float yaw     = mc.player.getYaw();
        // Minecraft yaw: 0=güney, pozitif=batıya dönüş, negatif=doğuya dönüş
        // 0-360 normalize et
        float yawNorm = ((yaw % 360) + 360) % 360;
        float halfVis = (STRIP_W / 2f) / PX_PER_DEG;

        // ── Tick & cardinal çizimi ─────────────────────────────────────────
        String[] cardinals = {"S","SW","W","NW","N","NE","E","SE"};
        float[]  cDegs     = {0,  45,  90, 135, 180,225, 270,315};

        for (float deg = yawNorm - halfVis - 10; deg <= yawNorm + halfVis + 10; deg++) {
            float norm  = ((deg % 360) + 360) % 360;
            int   iDeg  = Math.round(norm) % 360;
            float delta = norm - yawNorm;
            if (delta >  180) delta -= 360;
            if (delta < -180) delta += 360;
            int px2 = centerX + (int)(delta * PX_PER_DEG);
            if (px2 < stripX + 1 || px2 > stripX + STRIP_W - 2) continue;

            boolean isMaj  = (iDeg % 45 == 0);
            boolean isNorth= (iDeg == 180);

            if (iDeg % 10 == 0) {
                int tH    = isMaj ? 10 : 5;
                int tCol  = isMaj ? (isNorth ? C_NORTH : C_TICK_MAJ) : C_TICK_MIN;
                ctx.fill(px2, stripY + 2, px2 + 1, stripY + 2 + tH, tCol);

                if (isMaj) {
                    for (int ci = 0; ci < cDegs.length; ci++) {
                        if (Math.abs(cDegs[ci] - iDeg) < 0.5f) {
                            String lbl = cardinals[ci];
                            int lx = px2 - mc.textRenderer.getWidth(lbl) / 2;
                            int ly = stripY + 2 + tH + 1;
                            if (ly + mc.textRenderer.fontHeight < stripY + STRIP_H)
                                ctx.drawTextWithShadow(mc.textRenderer, Text.literal(lbl),
                                        lx, ly, isNorth ? C_NORTH : C_TICK_MAJ);
                            break;
                        }
                    }
                }
            }
        }

        // ── Hedef açısı hesapla ────────────────────────────────────────────
        // Minecraft yaw: 0=güney(+Z), -90=doğu(+X), 90=batı(-X), ±180=kuzey(-Z)
        // Hedefin oyuncuya göre açısı:
        double dX = activeTarget.getX() - px;
        double dZ = activeTarget.getZ() - pz;
        // atan2(dX, dZ): dZ>0 güney, dX>0 doğu — Minecraft yaw ile aynı sistem
        float targetYaw = (float) Math.toDegrees(Math.atan2(dX, dZ));
        targetYaw = ((targetYaw % 360) + 360) % 360;

        float markerDelta = targetYaw - yawNorm;
        if (markerDelta >  180) markerDelta -= 360;
        if (markerDelta < -180) markerDelta += 360;

        int markerX = centerX + (int)(markerDelta * PX_PER_DEG);
        boolean inStrip = markerX >= stripX + 4 && markerX <= stripX + STRIP_W - 5;

        if (inStrip) {
            // ── Hedef strip içinde: belirgin işaretçi ─────────────────────
            // Glow efekti (yarı saydam geniş şerit)
            ctx.fill(markerX - 3, stripY + 1, markerX + 4, stripY + STRIP_H - 1, 0x2255FFFF);
            ctx.fill(markerX - 1, stripY + 1, markerX + 2, stripY + STRIP_H - 1, 0x5555FFFF);
            // Ana çizgi
            ctx.fill(markerX,     stripY + 1, markerX + 1, stripY + STRIP_H - 1, C_TARGET);
            // Üst üçgen (▼ işareti)
            ctx.fill(markerX - 1, stripY + 1, markerX + 2, stripY + 2, C_TARGET);
            ctx.fill(markerX,     stripY + 2, markerX + 1, stripY + 3, C_TARGET);
        } else {
            // ── Hedef strip dışında: büyük kenar oku ──────────────────────
            boolean left = markerDelta < 0;
            int ax = left ? stripX + 3 : stripX + STRIP_W - 10;

            // Ok gövdesi
            if (left) {
                // ◀ ok
                for (int i = 0; i < 5; i++) {
                    ctx.fill(ax + i, stripY + STRIP_H/2 - i,
                             ax + i + 1, stripY + STRIP_H/2 + i + 1, C_TARGET);
                }
            } else {
                // ▶ ok
                for (int i = 0; i < 5; i++) {
                    ctx.fill(ax + 4 - i, stripY + STRIP_H/2 - i,
                             ax + 5 - i, stripY + STRIP_H/2 + i + 1, C_TARGET);
                }
            }
        }

        // ── Merkez işaretçi ▼ ─────────────────────────────────────────────
        ctx.fill(centerX,     stripY + STRIP_H - 3, centerX + 1, stripY + STRIP_H - 1, C_CENTER);
        ctx.fill(centerX - 1, stripY + STRIP_H - 4, centerX + 2, stripY + STRIP_H - 3, C_CENTER);
        ctx.fill(centerX - 2, stripY + STRIP_H - 5, centerX + 3, stripY + STRIP_H - 4, C_CENTER);

        // ── Alt bilgi şeridi ──────────────────────────────────────────────
        String name    = ChestStorage.getInstance().getDisplayName(activeTarget);
        String distStr = (int) dist + "m";
        String dir     = inStrip ? "" : (markerDelta < 0 ? " ◀" : " ▶");
        String info    = name + "  " + distStr + dir;

        int infoW = mc.textRenderer.getWidth(info) + 12;
        int infoX = (sw - infoW) / 2;
        int infoY = stripY + STRIP_H + 1;

        ctx.fill(infoX, infoY, infoX + infoW, infoY + INFO_H, C_INFO_BG);
        ctx.fill(infoX, infoY, infoX + infoW, infoY + 1, 0xFF333333);
        ctx.fill(infoX, infoY + INFO_H - 1, infoX + infoW, infoY + INFO_H, 0xFF333333);

        int nameW = mc.textRenderer.getWidth(name);
        int sepW  = mc.textRenderer.getWidth("  ");
        int ty    = infoY + (INFO_H - mc.textRenderer.fontHeight) / 2;
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(name),   infoX + 6,            ty, C_NAME);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal("  "),   infoX + 6 + nameW,    ty, C_TICK_MIN);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(distStr + dir), infoX + 6 + nameW + sepW, ty, C_DIST);
    }

    // ── Toast ─────────────────────────────────────────────────────────────
    private static void renderToasts(DrawContext ctx, MinecraftClient mc, long now) {
        if (toasts.isEmpty()) return;

        int sw    = mc.getWindow().getScaledWidth();
        int baseY = 8;
        int gap   = 3;
        int padX  = 8;
        int padY  = 3;

        int i = 0;
        for (Toast t : toasts) {
            float alpha = t.alpha(now);
            int   a     = (int)(alpha * 255);
            if (a <= 0) { i++; continue; }

            String titleStr = Text.translatable(t.titleKey()).getString();
            String line = (t.subtitle() != null && !t.subtitle().isBlank())
                    ? titleStr + "  •  " + t.subtitle() : titleStr;
            if (t.count() > 1) line = line + "  ×" + t.count();

            int textW = mc.textRenderer.getWidth(line);
            int boxW  = textW + padX * 2 + 4;
            int boxH  = mc.textRenderer.fontHeight + padY * 2;
            int boxX  = (sw - boxW) / 2;
            int boxY  = baseY + i * (boxH + gap);

            int accent = t.type() == ToastType.SUCCESS ? 0x55AA00 : 0x5555FF;
            ctx.fill(boxX, boxY, boxX + 2, boxY + boxH, withA(accent, a));
            ctx.drawTextWithShadow(mc.textRenderer,
                    Text.literal(line),
                    boxX + padX, boxY + padY,
                    withA(0xFFFFFF, a));
            i++;
        }
    }

    private static int withA(int rgb, int a) {
        return (Math.min(255, Math.max(0, a)) << 24) | (rgb & 0x00FFFFFF);
    }
}