package com.muhofy.chestmemory.ui;

import com.muhofy.chestmemory.data.ChestItem;
import com.muhofy.chestmemory.data.ChestRecord;
import com.muhofy.chestmemory.data.ChestStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ChestRecordsScreen extends Screen {

    // ── Renkler ───────────────────────────────────────────────────────────
    private static final int C_OVERLAY     = 0xCC000000;
    private static final int C_BG          = 0xEE1a1a1a;
    private static final int C_BG_PANEL    = 0xEE111111;
    private static final int C_BG_HEADER   = 0xFF0d0d0d;
    private static final int C_BG_ROW      = 0x661a1a1a;
    private static final int C_BG_ROW_SEL  = 0x9955FFFF;
    private static final int C_BG_ROW_HOV  = 0x44FFFFFF;
    private static final int C_BG_SLOT     = 0xFF2a2a2a;
    private static final int C_BG_FOOTER   = 0xFF0d0d0d;
    private static final int C_BORDER      = 0xFF2a2a2a;
    private static final int C_BORDER_TOP  = 0xFF1e2e2e;
    private static final int C_BORDER_ACC  = 0xFF55FFFF;
    private static final int C_DIVIDER     = 0xFF222222;
    private static final int C_SLOT_DARK   = 0xFF1a1a1a;
    private static final int C_SLOT_LIGHT  = 0xFF3a3a3a;
    private static final int C_TEXT        = 0xFFEEEEEE;
    private static final int C_TEXT_DIM    = 0xFF666666;
    private static final int C_TEXT_GRAY   = 0xFF999999;
    private static final int C_CYAN        = 0xFF55FFFF;
    private static final int C_YELLOW      = 0xFFFFFF55;
    private static final int C_GREEN       = 0xFF55FF55;
    private static final int C_RED         = 0xFFFF5555;

    // ── Layout ────────────────────────────────────────────────────────────
    private static final int TITLE_H = 22;
    private static final int FOOT_H  = 46;  // mesaj + buton için yeterli
    private static final int ROW_H   = 42;
    private static final int SLOT_S  = 18;
    private static final int SLOT_G  = 2;
    private static final int GRID_PAD= 8;

    private int POP_W, POP_H, LEFT_W;
    private int px, py;

    // ── State ─────────────────────────────────────────────────────────────
    private List<ChestRecord> chests     = new ArrayList<>();
    private int  selIdx       = 0;
    private int  scrollOffset = 0;
    private int  renamingIdx  = -1;
    private boolean confirmDel = false;

    private TextFieldWidget  renameField;
    private ButtonWidget     btnNav, btnDel, btnYes, btnNo;
    private final List<ButtonWidget> rowBtns    = new ArrayList<>();
    private final List<ButtonWidget> renameBtns = new ArrayList<>();

    public ChestRecordsScreen() {
        super(Text.translatable("stashfinder.records.title"));
    }

    // ── Init ──────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        POP_W  = Math.min(520, (int)(width  * 0.65f));
        POP_H  = Math.min(360, (int)(height * 0.75f));
        LEFT_W = (int)(POP_W * 0.34f);
        px = (width  - POP_W) / 2;
        py = (height - POP_H) / 2;
        refreshChests();
        buildActionButtons();
        buildRowButtons();
    }

    // ── Butonlar ──────────────────────────────────────────────────────────
    private void buildActionButtons() {
        int rpX  = px + LEFT_W + 2;
        int rpW  = POP_W - LEFT_W - 4;
        // Butonlar footer'ın alt kısmına sabitlendi
        int btnY = py + POP_H - 20;

        String navText = Text.translatable("stashfinder.records.btn.navigate").getString();
        String delText = Text.translatable("stashfinder.records.btn.delete").getString();

        int navW = textRenderer.getWidth(navText) + 16;
        int delW = textRenderer.getWidth(delText) + 16;

        btnNav = ButtonWidget.builder(Text.literal(navText), b -> doNavigate())
                .dimensions(rpX + 8, btnY, navW, 16).build();

        btnDel = ButtonWidget.builder(Text.literal(delText),
                b -> { confirmDel = true; syncBtns(); })
                .dimensions(rpX + rpW - delW - 8, btnY, delW, 16).build();

        btnYes = ButtonWidget.builder(
                Text.translatable("stashfinder.records.btn.confirm_yes"), b -> doDelete())
                .dimensions(rpX + rpW / 2 - 56, btnY, 54, 16).build();

        btnNo = ButtonWidget.builder(
                Text.translatable("stashfinder.records.btn.confirm_no"),
                b -> { confirmDel = false; syncBtns(); })
                .dimensions(rpX + rpW / 2 + 2, btnY, 54, 16).build();

        addDrawableChild(btnNav);
        addDrawableChild(btnDel);
        addDrawableChild(btnYes);
        addDrawableChild(btnNo);
        syncBtns();
    }

    private void buildRowButtons() {
        rowBtns.forEach(this::remove);
        renameBtns.forEach(this::remove);
        rowBtns.clear();
        renameBtns.clear();

        int listY   = py + TITLE_H + 1;
        int listH   = POP_H - TITLE_H - FOOT_H - 1;
        int visible = listH / ROW_H;

        for (int i = 0; i < visible; i++) {
            final int pos = i;
            int ry = listY + i * ROW_H;

            ButtonWidget sel = ButtonWidget.builder(Text.empty(), b -> {
                int idx = scrollOffset + pos;
                if (idx < chests.size()) {
                    selIdx = idx; cancelRename();
                    confirmDel = false; syncBtns();
                }
            }).dimensions(px, ry, LEFT_W - 20, ROW_H).build();
            sel.setAlpha(0f);
            rowBtns.add(sel);
            addDrawableChild(sel);

            ButtonWidget ren = ButtonWidget.builder(Text.empty(), b -> {
                int idx = scrollOffset + pos;
                if (idx < chests.size()) startRename(idx);
            }).dimensions(px + LEFT_W - 20, ry, 20, ROW_H).build();
            ren.setAlpha(0f);
            renameBtns.add(ren);
            addDrawableChild(ren);
        }
    }

    private void syncBtns() {
        boolean has = !chests.isEmpty();
        btnNav.visible = has && !confirmDel;
        btnDel.visible = has && !confirmDel;
        btnYes.visible = has &&  confirmDel;
        btnNo.visible  = has &&  confirmDel;
    }

    private void refreshChests() {
        chests = new ArrayList<>(ChestStorage.getInstance().getAll());
        if (selIdx >= chests.size()) selIdx = Math.max(0, chests.size() - 1);
    }

    // ── Rename ────────────────────────────────────────────────────────────
    private void startRename(int idx) {
        renamingIdx = idx;
        ChestRecord rec = chests.get(idx);
        String current  = rec.getCustomName() != null ? rec.getCustomName() : "";
        if (renameField != null) remove(renameField);
        renameField = new TextFieldWidget(textRenderer, 0, 0, 1, 1, null, Text.empty());
        renameField.setMaxLength(32);
        renameField.setText(current);
        renameField.setDrawsBackground(false);
        addDrawableChild(renameField);
        setFocused(renameField);
        renameField.setFocused(true);
    }

    private void cancelRename() {
        renamingIdx = -1;
        if (renameField != null) { remove(renameField); renameField = null; }
    }

    private void commitRename() {
        if (renamingIdx < 0 || renamingIdx >= chests.size()) return;
        String newName = renameField != null ? renameField.getText().trim() : "";
        ChestStorage.getInstance().rename(chests.get(renamingIdx).getId(),
                newName.isEmpty() ? null : newName);
        refreshChests();
        cancelRename();
    }

    // ── Navigate / Delete ─────────────────────────────────────────────────
    private void doNavigate() {
        if (chests.isEmpty()) return;
        ChestMemoryHud.setTarget(chests.get(selIdx));
        close();
    }

    private void doDelete() {
        if (chests.isEmpty()) return;
        ChestStorage.getInstance().delete(chests.get(selIdx).getId());
        refreshChests();
        confirmDel = false;
        syncBtns();
    }

    // ── Render ────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (renamingIdx >= 0 && renameField != null && getFocused() != renameField) {
            setFocused(renameField);
            renameField.setFocused(true);
        }

        ctx.fill(0, 0, width, height, C_OVERLAY);
        ctx.fill(px - 1, py - 1, px + POP_W + 1, py + POP_H + 1, 0xFF2a2a2a);
        ctx.fill(px, py, px + POP_W, py + POP_H, C_BG);
        ctx.fill(px, py, px + POP_W, py + 1, C_BORDER_TOP);
        ctx.fill(px,           py, px + 1,         py + POP_H, C_BORDER);
        ctx.fill(px + POP_W-1, py, px + POP_W,     py + POP_H, C_BORDER);
        ctx.fill(px, py + POP_H-1, px + POP_W,     py + POP_H, C_BORDER);

        renderTitleBar(ctx);
        renderLeftPanel(ctx, mouseX, mouseY);
        renderDivider(ctx);
        renderRightPanel(ctx, mouseX, mouseY);
        renderFooter(ctx);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Başlık ────────────────────────────────────────────────────────────
    private void renderTitleBar(DrawContext ctx) {
        ctx.fill(px + 1, py + 1, px + POP_W - 1, py + TITLE_H, C_BG_HEADER);
        ctx.fill(px + 1, py + TITLE_H, px + POP_W - 1, py + TITLE_H + 1, C_DIVIDER);

        IconManager im = IconManager.get();
        int iconX = px + 8;
        int textX = iconX;
        if (im.hasPng("chest")) {
            im.draw(ctx, "chest", iconX, py + (TITLE_H - 16) / 2 + 1);
            textX = iconX + 20;
        }
        String title = Text.translatable("stashfinder.records.title").getString()
                + "  (" + chests.size() + ")";
        ctx.drawTextWithShadow(textRenderer, Text.literal(title),
                textX, py + (TITLE_H - textRenderer.fontHeight) / 2, C_TEXT_GRAY);

        String esc = "ESC";
        ctx.drawTextWithShadow(textRenderer, Text.literal(esc),
                px + POP_W - textRenderer.getWidth(esc) - 8,
                py + (TITLE_H - textRenderer.fontHeight) / 2, C_TEXT_DIM);
    }

    // ── Sol Panel ─────────────────────────────────────────────────────────
    private void renderLeftPanel(DrawContext ctx, int mouseX, int mouseY) {
        int listY   = py + TITLE_H + 1;
        int listH   = POP_H - TITLE_H - FOOT_H - 1;
        int visible = listH / ROW_H;

        ctx.fill(px + 1, listY, px + LEFT_W, listY + listH, C_BG_PANEL);

        if (chests.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("stashfinder.records.empty_list"),
                    px + LEFT_W / 2, listY + listH / 2 - 4, C_TEXT_DIM);
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        String activeDim   = mc.world != null ? mc.world.getRegistryKey().getValue().toString() : "";
        double ppx         = mc.player != null ? mc.player.getX() : 0;
        double ppz         = mc.player != null ? mc.player.getZ() : 0;
        String diffStr     = Text.translatable("stashfinder.records.different_dimension").getString();
        String blkStr      = Text.translatable("stashfinder.chest.blk").getString();
        IconManager im     = IconManager.get();

        for (int i = scrollOffset; i < Math.min(chests.size(), scrollOffset + visible); i++) {
            ChestRecord rec = chests.get(i);
            int ry    = listY + (i - scrollOffset) * ROW_H;
            boolean sel  = (i == selIdx);
            boolean diff = !rec.isInDimension(activeDim);
            boolean hov  = !sel && mouseX >= px + 1 && mouseX < px + LEFT_W
                        && mouseY >= ry && mouseY < ry + ROW_H;

            if (sel) {
                ctx.fill(px + 1, ry, px + LEFT_W, ry + ROW_H, C_BG_ROW_SEL);
                ctx.fill(px + 1, ry, px + 3, ry + ROW_H, C_CYAN);
            } else if (hov) {
                ctx.fill(px + 1, ry, px + LEFT_W, ry + ROW_H, C_BG_ROW_HOV);
            } else {
                ctx.fill(px + 1, ry, px + LEFT_W, ry + ROW_H, C_BG_ROW);
            }

            String icName = rec.isDouble() ? "chest_double" : "chest";
            int iconX = px + 6, iconY = ry + (ROW_H - 16) / 2;
            if (im.hasPng(icName)) {
                im.draw(ctx, icName, iconX, iconY);
            } else {
                int dotColor = diff ? C_TEXT_DIM : (sel ? C_CYAN : C_GREEN);
                ctx.fill(iconX + 2, iconY + 4, iconX + 8, iconY + 10, dotColor);
            }

            int textX = px + 26;

            if (renamingIdx == i && renameField != null) {
                String rtext = renameField.getText();
                int textY    = ry + 6;
                int fieldX   = px + 26;
                int maxW     = LEFT_W - 46;
                ctx.fill(fieldX - 2, textY - 2, fieldX + maxW + 2,
                        textY + textRenderer.fontHeight + 2, 0xFF111111);
                ctx.fill(fieldX - 2, textY + textRenderer.fontHeight + 2,
                        fieldX + maxW + 2, textY + textRenderer.fontHeight + 3, C_CYAN);
                if (rtext.isEmpty()) {
                    ctx.drawTextWithShadow(textRenderer,
                            Text.literal("İsim gir..."), fieldX, textY, C_TEXT_DIM);
                } else {
                    String vis = textRenderer.getWidth(rtext) > maxW
                            ? textRenderer.trimToWidth(rtext, maxW, true) : rtext;
                    ctx.drawTextWithShadow(textRenderer, Text.literal(vis), fieldX, textY, 0xFFFFFFFF);
                }
                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    String cur = textRenderer.getWidth(renameField.getText()) > maxW
                            ? textRenderer.trimToWidth(renameField.getText(), maxW, true)
                            : renameField.getText();
                    int cursorX = fieldX + textRenderer.getWidth(cur);
                    ctx.fill(cursorX, textY - 1, cursorX + 1,
                            textY + textRenderer.fontHeight + 1, 0xFFFFFFFF);
                }
            } else {
                String name = ChestStorage.getInstance().getDisplayName(rec);
                String disp = textRenderer.getWidth(name) > LEFT_W - 46
                        ? textRenderer.trimToWidth(name, LEFT_W - 50) + "…" : name;
                ctx.drawTextWithShadow(textRenderer, Text.literal(disp),
                        textX, ry + 7,
                        diff ? C_TEXT_DIM : (sel ? 0xFF0d0d0d : C_TEXT));
            }

            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(rec.getX() + ", " + rec.getY() + ", " + rec.getZ()),
                    textX, ry + 18, diff ? C_TEXT_DIM : (sel ? 0xFF1a4a4a : C_TEXT_DIM));

            String dist = diff ? diffStr : ((int) rec.distanceTo(ppx, ppz)) + blkStr;
            ctx.drawTextWithShadow(textRenderer, Text.literal(dist),
                    textX, ry + 29, diff ? C_TEXT_DIM : (sel ? 0xFF006666 : C_CYAN));

            boolean hRen = mouseX >= px + LEFT_W - 20 && mouseX < px + LEFT_W
                    && mouseY >= ry && mouseY < ry + ROW_H;
            if (im.hasPng("rename")) {
                im.draw(ctx, "rename", px + LEFT_W - 18, ry + (ROW_H - 16) / 2);
            } else {
                ctx.drawTextWithShadow(textRenderer,
                        Text.literal(im.fallback("rename")),
                        px + LEFT_W - 14, ry + (ROW_H - textRenderer.fontHeight) / 2,
                        hRen ? C_CYAN : C_TEXT_DIM);
            }

            ctx.fill(px + 1, ry + ROW_H - 1, px + LEFT_W, ry + ROW_H, C_DIVIDER);
        }
    }

    // ── Divider ───────────────────────────────────────────────────────────
    private void renderDivider(DrawContext ctx) {
        int top = py + TITLE_H + 1;
        int bot = py + POP_H - FOOT_H;
        ctx.fill(px + LEFT_W, top, px + LEFT_W + 1, bot, C_DIVIDER);
    }

    // ── Sağ Panel ─────────────────────────────────────────────────────────
    private void renderRightPanel(DrawContext ctx, int mouseX, int mouseY) {
        int rpX = px + LEFT_W + 2;
        int rpW = POP_W - LEFT_W - 4;
        int rpY = py + TITLE_H + 1;

        if (chests.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("stashfinder.records.select_hint"),
                    rpX + rpW / 2, py + (POP_H - FOOT_H) / 2, C_TEXT_DIM);
            return;
        }

        ChestRecord rec   = chests.get(selIdx);
        String      name  = ChestStorage.getInstance().getDisplayName(rec);
        int         slots = rec.getSlotCount();
        int         rows  = slots / 9;

        ctx.fill(rpX, rpY, rpX + rpW, rpY + TITLE_H - 2, C_BG_HEADER);
        ctx.fill(rpX, rpY + TITLE_H - 2, rpX + rpW, rpY + TITLE_H - 1, C_DIVIDER);
        ctx.drawTextWithShadow(textRenderer, Text.literal(name),
                rpX + 6, rpY + (TITLE_H - 2 - textRenderer.fontHeight) / 2, C_TEXT);

        String dimShort   = shortDim(rec.getDimension());
        String dimIconKey = dimIconName(rec.getDimension());
        IconManager im    = IconManager.get();
        boolean hasDimIcon= im.hasPng(dimIconKey);

        int bw     = textRenderer.getWidth(dimShort) + (hasDimIcon ? 22 : 10);
        int bx     = rpX + rpW - bw - 4;
        int badgeY = rpY + (TITLE_H - 2 - (textRenderer.fontHeight + 6)) / 2;
        int badgeH = textRenderer.fontHeight + 6;

        ctx.fill(bx, badgeY, bx + bw, badgeY + badgeH, 0xFF111111);
        ctx.fill(bx, badgeY, bx + bw, badgeY + 1, C_BORDER_TOP);
        if (hasDimIcon) {
            im.draw(ctx, dimIconKey, bx + 3, badgeY + (badgeH - 16) / 2);
            ctx.drawTextWithShadow(textRenderer, Text.literal(dimShort),
                    bx + 20, badgeY + (badgeH - textRenderer.fontHeight) / 2, C_TEXT_GRAY);
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal(dimShort),
                    bx + 5, badgeY + (badgeH - textRenderer.fontHeight) / 2, C_TEXT_GRAY);
        }

        int bodyY = rpY + TITLE_H + 4;
        String slotLabel = Text.translatable(rec.isDouble()
                ? "stashfinder.records.slot_label_double"
                : "stashfinder.records.slot_label_single").getString();
        ctx.drawTextWithShadow(textRenderer, Text.literal(slotLabel),
                rpX + GRID_PAD, bodyY, C_TEXT_DIM);
        bodyY += textRenderer.fontHeight + 4;

        int gridX = rpX + GRID_PAD;
        List<ChestItem> items = rec.getItems();

        for (int slot = 0; slot < slots; slot++) {
            int col    = slot % 9;
            int row    = slot / 9;
            int extraY = (rec.isDouble() && row >= 3) ? 4 : 0;
            int sx     = gridX + col * (SLOT_S + SLOT_G);
            int sy     = bodyY + row * (SLOT_S + SLOT_G) + extraY;

            slotBox(ctx, sx, sy, SLOT_S);

            ChestItem ci = itemForSlot(items, slot);
            if (ci != null) {
                ItemStack stack = buildStack(ci.getItemId());
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, sx + 1, sy + 1);
                    if (ci.getCount() > 1) {
                        String c = String.valueOf(ci.getCount());
                        ctx.drawTextWithShadow(textRenderer, Text.literal(c),
                                sx + SLOT_S - textRenderer.getWidth(c),
                                sy + SLOT_S - textRenderer.fontHeight + 1, C_YELLOW);
                    }
                }
                if (mouseX >= sx && mouseX < sx + SLOT_S
                        && mouseY >= sy && mouseY < sy + SLOT_S)
                    ctx.drawTooltip(textRenderer,
                            Text.literal(ci.getDisplayName() + " ×" + ci.getCount()),
                            mouseX, mouseY);
            }
        }

        int metaY = bodyY + rows * (SLOT_S + SLOT_G) + (rec.isDouble() ? 7 : 4);
        if (metaY < py + POP_H - FOOT_H - textRenderer.fontHeight - 2) {
            String upd = Text.translatable("stashfinder.records.last_updated").getString()
                    + (rec.getLastUpdated() != null
                    ? rec.getLastUpdated().substring(0, Math.min(16, rec.getLastUpdated().length()))
                    : "?");
            ctx.drawTextWithShadow(textRenderer, Text.literal(upd),
                    rpX + GRID_PAD, metaY, C_TEXT_DIM);
        }
    }

    // ── Footer ────────────────────────────────────────────────────────────
    private void renderFooter(DrawContext ctx) {
        int fy  = py + POP_H - FOOT_H;
        int rpX = px + LEFT_W + 2;
        int rpW = POP_W - LEFT_W - 4;

        // Arka plan
        ctx.fill(px + 1, fy, px + POP_W - 1, py + POP_H - 1, C_BG_FOOTER);
        ctx.fill(px + 1, fy, px + POP_W - 1, fy + 1, C_DIVIDER);

        if (confirmDel) {
            // Confirm mesajı — butonların hemen üstünde
            String msg = Text.translatable("stashfinder.records.confirm_delete").getString();
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(msg),
                    rpX + rpW / 2,
                    fy + 8,
                    C_RED);
            // Butonlar (btnYes / btnNo) buildActionButtons'da py + POP_H - 20 konumunda
        }
    }

    // ── Klavye ────────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();

        if (renamingIdx >= 0) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                commitRename(); return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                cancelRename(); return true;
            }
            return super.keyPressed(input);
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (confirmDel) { confirmDel = false; syncBtns(); return true; }
            close(); return true;
        }

        if (key == GLFW.GLFW_KEY_UP && selIdx > 0) {
            selIdx--; scrollIntoView(); return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN && selIdx < chests.size() - 1) {
            selIdx++; scrollIntoView(); return true;
        }

        return super.keyPressed(input);
    }

    private void scrollIntoView() {
        int listH   = POP_H - TITLE_H - FOOT_H - 1;
        int visible = listH / ROW_H;
        if (selIdx < scrollOffset) scrollOffset = selIdx;
        if (selIdx >= scrollOffset + visible) scrollOffset = selIdx - visible + 1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int listH   = POP_H - TITLE_H - FOOT_H - 1;
        int visible = listH / ROW_H;
        int maxOff  = Math.max(0, chests.size() - visible);
        scrollOffset = (int) Math.max(0, Math.min(maxOff, scrollOffset - vertical));
        buildRowButtons();
        return true;
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────
    private void slotBox(DrawContext ctx, int x, int y, int s) {
        ctx.fill(x,     y,     x+s, y+s, C_BG_SLOT);
        ctx.fill(x,     y,     x+s, y+1, C_SLOT_DARK);
        ctx.fill(x,     y,     x+1, y+s, C_SLOT_DARK);
        ctx.fill(x,     y+s-1, x+s, y+s, C_SLOT_LIGHT);
        ctx.fill(x+s-1, y,     x+s, y+s, C_SLOT_LIGHT);
    }

    private ChestItem itemForSlot(List<ChestItem> items, int slot) {
        for (ChestItem ci : items)
            if (ci.getSlot() == slot) return ci;
        return null;
    }

    private ItemStack buildStack(String itemId) {
        try { return Registries.ITEM.get(Identifier.of(itemId)).getDefaultStack(); }
        catch (Exception e) { return ItemStack.EMPTY; }
    }

    private String shortDim(String dim) {
        if (dim == null) return "?";
        if (dim.contains("overworld")) return "Overworld";
        if (dim.contains("nether"))    return "Nether";
        if (dim.contains("end"))       return "The End";
        return dim;
    }

    private String dimIconName(String dim) {
        if (dim == null) return "dim_overworld";
        if (dim.contains("nether")) return "dim_nether";
        if (dim.contains("end"))    return "dim_end";
        return "dim_overworld";
    }
}