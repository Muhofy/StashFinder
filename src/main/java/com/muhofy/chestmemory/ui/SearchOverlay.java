package com.muhofy.chestmemory.ui;

import com.muhofy.chestmemory.data.ChestItem;
import com.muhofy.chestmemory.data.ChestRecord;
import com.muhofy.chestmemory.data.ChestStorage;
import com.muhofy.chestmemory.data.ChestStorage.SortMode;
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

public class SearchOverlay extends Screen {

    // ── Renkler ───────────────────────────────────────────────────────────
    private static final int C_OVERLAY     = 0xCC000000;
    private static final int C_BG          = 0xEE1a1a1a;
    private static final int C_BG_INPUT    = 0xFF111111;
    private static final int C_BG_ROW      = 0x441a1a1a;
    private static final int C_BG_ROW_SEL  = 0x8855FFFF;
    private static final int C_BG_HISTORY  = 0x33FFFFFF;
    private static final int C_BG_FOOTER   = 0xFF0d0d0d;
    private static final int C_BG_SLOT     = 0xFF2a2a2a;
    private static final int C_BG_SORT     = 0xFF222222;
    private static final int C_BG_SORT_SEL = 0xFF1a3a3a;
    private static final int C_BORDER_ACC  = 0xFF55FFFF;
    private static final int C_BORDER      = 0xFF2a2a2a;
    private static final int C_DIVIDER     = 0xFF222222;
    private static final int C_SLOT_DARK   = 0xFF1a1a1a;
    private static final int C_SLOT_LIGHT  = 0xFF3a3a3a;
    private static final int C_TEXT        = 0xFFEEEEEE;
    private static final int C_TEXT_DIM    = 0xFF555555;
    private static final int C_TEXT_HIST   = 0xFF999999;
    private static final int C_CYAN        = 0xFF55FFFF;
    private static final int C_YELLOW      = 0xFFFFFF55;
    private static final int C_RED         = 0xFFFF5555;

    // ── Layout ────────────────────────────────────────────────────────────
    private static final int BOX_W         = 360;
    private static final int INPUT_H       = 28;
    private static final int ROW_H         = 34;
    private static final int HIST_ROW_H    = 20;
    private static final int HIST_HEADER_H = 16;
    private static final int FOOTER_H      = 20;
    private static final int MAX_VISIBLE  = 4;  // ekranda görünen max satır
    private int scrollOffset = 0;               // kaç satır aşağı kaydırıldı
    private static final int MAX_HIST_SHOW = 5;
    private static final int SORT_ROW_H    = 18;
    private static final int SORT_COUNT    = 4;

    // Sort seçenekleri
    private static final SortMode[] SORT_MODES = {
        SortMode.DISTANCE, SortMode.DATE, SortMode.COUNT, SortMode.NAME
    };
    private static final String[] SORT_ICONS  = { "📍", "📅", "🔢", "🔤" };
    private static final String[] SORT_KEYS   = {
        "stashfinder.sort.distance",
        "stashfinder.sort.date",
        "stashfinder.sort.count",
        "stashfinder.sort.name"
    };

    // ── State ─────────────────────────────────────────────────────────────
    private TextFieldWidget searchField;
    private List<ChestStorage.SearchResult> results  = new ArrayList<>();
    private List<String>                    history  = new ArrayList<>();
    private int     selectedIndex = 0;
    private boolean showHistory   = true;
    private boolean showSortMenu  = false;

    private final List<ButtonWidget> resultButtons  = new ArrayList<>();
    private final List<ButtonWidget> historyButtons = new ArrayList<>();
    private final List<ButtonWidget> deleteButtons  = new ArrayList<>();
    private final List<ButtonWidget> sortButtons    = new ArrayList<>();
    private ButtonWidget             sortToggleBtn;

    public SearchOverlay() {
        super(Text.translatable("stashfinder.search.placeholder"));
    }

    // ── Box yüksekliği ────────────────────────────────────────────────────
    private int visibleHistCount() { return Math.min(history.size(), MAX_HIST_SHOW); }

    private int boxH() {
        if (showHistory) {
            if (history.isEmpty()) return INPUT_H + 1 + 32 + FOOTER_H;
            return INPUT_H + 1 + HIST_HEADER_H + visibleHistCount() * HIST_ROW_H + FOOTER_H;
        }
        // Her zaman MAX_VISIBLE satır yüksekliği — results.size()'a bakma
        int rows = results.isEmpty() ? 1 : MAX_VISIBLE;
        return INPUT_H + 1 + rows * ROW_H + FOOTER_H;
    }

    private int boxX() { return (width  - BOX_W) / 2; }
    private int boxY() { return (height - boxH()) / 2; }

    // ── Init ──────────────────────────────────────────────────────────────
    private ButtonWidget cancelBtn;

    @Override
    protected void init() {
        history = new ArrayList<>(ChestStorage.getInstance().getSearchHistory());
        rebuildField();
        rebuildSortToggle();
        rebuildCancelBtn();
        rebuildButtons();
    }

    private void rebuildCancelBtn() {
        if (cancelBtn != null) remove(cancelBtn);
        int bx = boxX(), by = boxY(), bh = boxH();
        cancelBtn = ButtonWidget.builder(Text.empty(), b -> close())
                .dimensions(bx + BOX_W - 40, by + bh - FOOTER_H + 2, 34, FOOTER_H - 4)
                .build();
        cancelBtn.setAlpha(0f);
        addDrawableChild(cancelBtn);
    }

    private void rebuildField() {
        if (searchField != null) remove(searchField);
        int bx = boxX(), by = boxY();
        searchField = new TextFieldWidget(textRenderer,
                bx + 30, by + (INPUT_H - textRenderer.fontHeight) / 2,
                BOX_W - 70, textRenderer.fontHeight + 4,
                null, Text.translatable("stashfinder.search.placeholder"));
        searchField.setMaxLength(64);
        searchField.setPlaceholder(Text.translatable("stashfinder.search.placeholder"));
        searchField.setChangedListener(this::onSearchChanged);
        searchField.setDrawsBackground(false);
        searchField.setEditableColor(0xFFFFFF);
        searchField.active = true;
        addDrawableChild(searchField);
        setFocused(searchField);
        searchField.setFocused(true);
    }

    private void rebuildSortToggle() {
        if (sortToggleBtn != null) remove(sortToggleBtn);
        int bx = boxX(), by = boxY();
        // Input'un sağ tarafında, 38px genişliğinde
        sortToggleBtn = ButtonWidget.builder(Text.empty(), b -> {
            showSortMenu = !showSortMenu;
            rebuildButtons();
        }).dimensions(bx + BOX_W - 40, by + 5, 34, INPUT_H - 10).build();
        sortToggleBtn.setAlpha(0f);
        addDrawableChild(sortToggleBtn);
    }

    // ── Listener ──────────────────────────────────────────────────────────
    private void onSearchChanged(String query) {
        selectedIndex = 0;
        scrollOffset  = 0;
        showHistory   = query.isBlank();
        showSortMenu  = false;

        if (showHistory) {
            results = new ArrayList<>();
            scrollOffset = 0;
            rebuildButtons();
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            results = new ArrayList<>();
            rebuildButtons();
            return;
        }
        String dim = mc.world.getRegistryKey().getValue().toString();
        results    = ChestStorage.getInstance().searchItems(query, dim,
                mc.player.getX(), mc.player.getZ());
        // MAX_RESULTS sınırı kaldırıldı — scroll ile tümünü göster
        rebuildButtons();
    }

    private void refreshResults() {
        if (searchField == null || searchField.getText().isBlank()) return;
        onSearchChanged(searchField.getText());
    }

    // ── Butonlar ──────────────────────────────────────────────────────────
    private void rebuildButtons() {
        resultButtons.forEach(this::remove);
        historyButtons.forEach(this::remove);
        deleteButtons.forEach(this::remove);
        sortButtons.forEach(this::remove);
        resultButtons.clear();
        historyButtons.clear();
        deleteButtons.clear();
        sortButtons.clear();

        int bx = boxX(), by = boxY();

        // Sort dropdown butonları
        if (showSortMenu) {
            int sx  = bx + BOX_W - 130;
            int sy0 = by + INPUT_H + 2;
            for (int i = 0; i < SORT_COUNT; i++) {
                final int idx = i;
                ButtonWidget btn = ButtonWidget.builder(Text.empty(), b -> {
                    ChestStorage.getInstance().setSortMode(SORT_MODES[idx]);
                    showSortMenu = false;
                    refreshResults();
                    rebuildButtons();
                }).dimensions(sx, sy0 + i * SORT_ROW_H, 128, SORT_ROW_H).build();
                btn.setAlpha(0f);
                sortButtons.add(btn);
                addDrawableChild(btn);
            }
            return; // sort menu açıkken diğer butonları ekleme
        }

        if (showHistory) {
            int hy0 = by + INPUT_H + 1 + HIST_HEADER_H;
            for (int i = 0; i < visibleHistCount(); i++) {
                final int idx = i;
                ButtonWidget row = ButtonWidget.builder(Text.empty(), b -> applyHistory(idx))
                        .dimensions(bx + 1, hy0 + idx * HIST_ROW_H, BOX_W - 22, HIST_ROW_H).build();
                row.setAlpha(0f);
                historyButtons.add(row);
                addDrawableChild(row);

                ButtonWidget del = ButtonWidget.builder(Text.empty(), b -> deleteHistory(idx))
                        .dimensions(bx + BOX_W - 20, hy0 + idx * HIST_ROW_H, 18, HIST_ROW_H).build();
                del.setAlpha(0f);
                deleteButtons.add(del);
                addDrawableChild(del);
            }
        } else {
            int ry0      = by + INPUT_H + 1;
            int visible  = Math.min(results.size() - scrollOffset, MAX_VISIBLE);
            for (int i = 0; i < visible; i++) {
                final int idx = scrollOffset + i;
                ButtonWidget btn = ButtonWidget.builder(Text.empty(), b -> {
                    selectedIndex = idx;
                    activateSelected();
                }).dimensions(bx + 1, ry0 + i * ROW_H, BOX_W - 2, ROW_H).build();
                btn.setAlpha(0f);
                resultButtons.add(btn);
                addDrawableChild(btn);
            }
        }
    }

    private void applyHistory(int idx) {
        if (idx >= history.size()) return;
        String q = history.get(idx);
        searchField.setText(q);
        searchField.setCursorToEnd(false);
        onSearchChanged(q);
    }

    private void deleteHistory(int idx) {
        if (idx >= history.size()) return;
        ChestStorage.getInstance().removeFromHistory(history.get(idx));
        history = new ArrayList<>(ChestStorage.getInstance().getSearchHistory());
        rebuildButtons();
    }

    // ── Render ────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, C_OVERLAY);

        int bx = boxX(), by = boxY(), bh = boxH();

        ctx.fill(bx - 1, by - 1, bx + BOX_W + 1, by + bh + 1, 0xFF333333);
        ctx.fill(bx, by, bx + BOX_W, by + bh, C_BG);
        ctx.fill(bx, by,              bx + BOX_W, by + 1,    C_BORDER_ACC);
        ctx.fill(bx,           by,    bx + 1,     by + bh,   C_BORDER);
        ctx.fill(bx + BOX_W-1, by,    bx + BOX_W, by + bh,   C_BORDER);
        ctx.fill(bx,  by + bh - 1,    bx + BOX_W, by + bh,   C_BORDER);

        renderInputRow(ctx, bx, by, mouseX, mouseY);
        ctx.fill(bx + 1, by + INPUT_H, bx + BOX_W - 1, by + INPUT_H + 1, C_DIVIDER);

        if (showSortMenu) {
            renderSortMenu(ctx, bx, by, mouseX, mouseY);
        } else if (showHistory) {
            renderHistory(ctx, bx, by, mouseX, mouseY);
        } else {
            renderResults(ctx, bx, by, mouseX, mouseY);
        }

        renderFooter(ctx, bx, by, bh);
        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Input satırı ──────────────────────────────────────────────────────
    private void renderInputRow(DrawContext ctx, int bx, int by, int mouseX, int mouseY) {
        ctx.fill(bx + 1, by + 1, bx + BOX_W - 1, by + INPUT_H, C_BG_INPUT);

        // Arama ikonu
        ctx.drawTextWithShadow(textRenderer, Text.literal("🔍"),
                bx + 8, by + (INPUT_H - textRenderer.fontHeight) / 2, C_TEXT_DIM);

        // Input metni
        if (searchField != null) {
            String text = searchField.getText();
            int textY   = by + (INPUT_H - textRenderer.fontHeight) / 2;
            int textX   = bx + 30;
            if (text.isEmpty()) {
                ctx.drawTextWithShadow(textRenderer,
                        Text.translatable("stashfinder.search.placeholder"),
                        textX, textY, C_TEXT_DIM);
            } else {
                ctx.drawTextWithShadow(textRenderer, Text.literal(text), textX, textY, 0xFFFFFFFF);
            }
            if (searchField.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cursorX = textX + textRenderer.getWidth(text);
                ctx.fill(cursorX, textY - 1, cursorX + 1,
                        textY + textRenderer.fontHeight + 1, 0xFFFFFFFF);
            }
        }

        // Sıralama butonu — sağ üst köşe
        SortMode mode    = ChestStorage.getInstance().getSortMode();
        int modeIdx      = List.of(SORT_MODES).indexOf(mode);
        int btnX = bx + BOX_W - 40;
        int btnY2 = by + 5;
        int btnW = 34, btnH = INPUT_H - 10;
        boolean sortHov  = mouseX >= btnX && mouseX < btnX + btnW
                        && mouseY >= btnY2 && mouseY < btnY2 + btnH;
        boolean sortAct  = showSortMenu;

        // Hover animasyonu
        int sortBg = sortAct ? 0xFF1e3535
                   : sortHov ? 0xFF2a2a2a
                   : 0xFF1a1a1a;
        ctx.fill(btnX, btnY2, btnX + btnW, btnY2 + btnH, sortBg);

        // Border
        int borderCol = sortAct ? C_BORDER_ACC
                      : sortHov ? 0xFF4a4a4a
                      : 0xFF2a2a2a;
        ctx.fill(btnX,          btnY2,          btnX + btnW, btnY2 + 1,       borderCol);
        ctx.fill(btnX,          btnY2 + btnH-1, btnX + btnW, btnY2 + btnH,    borderCol);
        ctx.fill(btnX,          btnY2,          btnX + 1,    btnY2 + btnH,    borderCol);
        ctx.fill(btnX + btnW-1, btnY2,          btnX + btnW, btnY2 + btnH,    borderCol);

        // Cyan left accent — aktifse
        if (sortAct)
            ctx.fill(btnX, btnY2, btnX + 2, btnY2 + btnH, C_BORDER_ACC);

        // PNG ikon varsa kullan, yoksa fallback unicode
        IconManager im = IconManager.get();
        int iconTextY  = btnY2 + (btnH - textRenderer.fontHeight) / 2;
        int iconColor  = sortAct ? C_CYAN : sortHov ? 0xFFCCCCCC : 0xFF777777;
        if (im.hasPng("sort")) {
            int iconY = btnY2 + (btnH - 10) / 2;
            im.draw(ctx, "sort", btnX + 4, iconY);
        } else {
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(SORT_ICONS[modeIdx]),
                    btnX + 5, iconTextY, iconColor);
        }
        // Dropdown oku
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(sortAct ? "▴" : "▾"),
                btnX + btnW - 10, iconTextY, iconColor);

        boolean hasText = searchField != null && !searchField.getText().isEmpty();
        ctx.fill(bx + 1, by + INPUT_H - 1, bx + BOX_W - 1, by + INPUT_H,
                hasText ? C_BORDER_ACC : C_DIVIDER);
    }

    // ── Sort menüsü ───────────────────────────────────────────────────────
    private void renderSortMenu(DrawContext ctx, int bx, int by, int mouseX, int mouseY) {
        SortMode current = ChestStorage.getInstance().getSortMode();
        int sx   = bx + BOX_W - 130;
        int sw   = 128;
        int sy0  = by + INPUT_H + 2;
        int totalH = SORT_COUNT * SORT_ROW_H;

        // Arka plan
        ctx.fill(sx - 1, sy0 - 1, sx + sw + 1, sy0 + totalH + 1, 0xFF333333);
        ctx.fill(sx, sy0, sx + sw, sy0 + totalH, 0xFF1a1a1a);

        for (int i = 0; i < SORT_COUNT; i++) {
            int ry  = sy0 + i * SORT_ROW_H;
            boolean sel = SORT_MODES[i] == current;
            boolean hov = mouseX >= sx && mouseX < sx + sw
                       && mouseY >= ry && mouseY < ry + SORT_ROW_H;

            if (sel) ctx.fill(sx, ry, sx + sw, ry + SORT_ROW_H, C_BG_SORT_SEL);
            else if (hov) ctx.fill(sx, ry, sx + sw, ry + SORT_ROW_H, 0xFF2a2a2a);

            if (sel) ctx.fill(sx, ry, sx + 2, ry + SORT_ROW_H, C_CYAN);

            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(SORT_ICONS[i] + " " + Text.translatable(SORT_KEYS[i]).getString()),
                    sx + 6, ry + (SORT_ROW_H - textRenderer.fontHeight) / 2,
                    sel ? C_CYAN : (hov ? C_TEXT : C_TEXT_HIST));

            if (i < SORT_COUNT - 1)
                ctx.fill(sx + 4, ry + SORT_ROW_H - 1, sx + sw - 4, ry + SORT_ROW_H, C_DIVIDER);
        }
    }

    // ── Geçmiş ────────────────────────────────────────────────────────────
    private void renderHistory(DrawContext ctx, int bx, int by, int mouseX, int mouseY) {
        int hy0 = by + INPUT_H + 1;

        if (history.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("stashfinder.search.hint"),
                    bx + BOX_W / 2, hy0 + 10, C_TEXT_DIM);
            return;
        }

        int headerY = hy0 + (HIST_HEADER_H - textRenderer.fontHeight) / 2;
        ctx.drawTextWithShadow(textRenderer,
                Text.translatable("stashfinder.search.history_label"),
                bx + 10, headerY, C_TEXT_DIM);
        ctx.fill(bx + 1, hy0 + HIST_HEADER_H - 1, bx + BOX_W - 1, hy0 + HIST_HEADER_H, C_DIVIDER);

        int listY = hy0 + HIST_HEADER_H;
        for (int i = 0; i < visibleHistCount(); i++) {
            int ry  = listY + i * HIST_ROW_H;
            boolean hov = mouseX >= bx + 1 && mouseX < bx + BOX_W - 20
                    && mouseY >= ry && mouseY < ry + HIST_ROW_H;

            if (hov) ctx.fill(bx + 1, ry, bx + BOX_W - 1, ry + HIST_ROW_H, C_BG_HISTORY);

            ctx.drawTextWithShadow(textRenderer, Text.literal("⌚"),
                    bx + 8, ry + (HIST_ROW_H - textRenderer.fontHeight) / 2, C_TEXT_DIM);
            ctx.drawTextWithShadow(textRenderer, Text.literal(history.get(i)),
                    bx + 24, ry + (HIST_ROW_H - textRenderer.fontHeight) / 2,
                    hov ? C_TEXT : C_TEXT_HIST);

            boolean hDel = mouseX >= bx + BOX_W - 20 && mouseX < bx + BOX_W - 2
                    && mouseY >= ry && mouseY < ry + HIST_ROW_H;
            ctx.drawTextWithShadow(textRenderer, Text.literal("✕"),
                    bx + BOX_W - 16, ry + (HIST_ROW_H - textRenderer.fontHeight) / 2,
                    hDel ? C_RED : C_TEXT_DIM);

            if (i < visibleHistCount() - 1)
                ctx.fill(bx + 8, ry + HIST_ROW_H - 1, bx + BOX_W - 8, ry + HIST_ROW_H, C_DIVIDER);
        }
    }

    // ── Sonuçlar ──────────────────────────────────────────────────────────
    private void renderResults(DrawContext ctx, int bx, int by, int mouseX, int mouseY) {
        int ry0    = by + INPUT_H + 1;
        int visible = Math.min(results.size() - scrollOffset, MAX_VISIBLE);
        int areaH  = Math.max(1, visible) * ROW_H;

        if (results.isEmpty()) {
            ctx.fill(bx + 1, ry0, bx + BOX_W - 1, ry0 + ROW_H, C_BG);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("stashfinder.search.empty"),
                    bx + BOX_W / 2, ry0 + ROW_H / 2 - 4, C_TEXT_DIM);
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        String activeDim   = mc.world != null ? mc.world.getRegistryKey().getValue().toString() : "";

        for (int i = 0; i < visible; i++) {
            int idx  = scrollOffset + i;
            int rowY = ry0 + i * ROW_H;
            ChestStorage.SearchResult r = results.get(idx);
            boolean sel  = (idx == selectedIndex);
            boolean diff = !r.chest.isInDimension(activeDim);

            if (sel) {
                ctx.fill(bx + 1, rowY, bx + BOX_W - 1, rowY + ROW_H, C_BG_ROW_SEL);
                ctx.fill(bx + 1, rowY, bx + 3, rowY + ROW_H, C_CYAN);
            } else {
                ctx.fill(bx + 1, rowY, bx + BOX_W - 1, rowY + ROW_H, C_BG_ROW);
            }

            int slotX = bx + 8, slotY = rowY + (ROW_H - 18) / 2;
            slotBox(ctx, slotX, slotY, 18);
            ItemStack stack = r.firstItem() != null
                    ? buildStack(r.firstItem().getItemId()) : ItemStack.EMPTY;
            if (!stack.isEmpty() && !diff)
                ctx.drawItem(stack, slotX + 1, slotY + 1);

            int textX = slotX + 22;
            int nameY = rowY + 7;
            int subY  = rowY + 19;

            String name = r.firstItem() != null && r.firstItem().getDisplayName() != null
                    ? r.firstItem().getDisplayName()
                    : Text.translatable("stashfinder.item.unknown").getString();
            String chestName = ChestStorage.getInstance().getDisplayName(r.chest);
            String sub       = chestName + "  " + r.chest.getX() + ", "
                             + r.chest.getY() + ", " + r.chest.getZ();

            ctx.drawTextWithShadow(textRenderer, Text.literal(name),
                    textX, nameY, diff ? C_TEXT_DIM : (sel ? C_CYAN : C_TEXT));
            ctx.drawTextWithShadow(textRenderer, Text.literal(sub),
                    textX, subY, C_TEXT_DIM);

            String countStr = r.totalCount + "x";
            String distStr  = diff
                    ? Text.translatable("stashfinder.records.different_dimension").getString()
                    : ((int) r.distance) + "m " + dirArrow(r.chest, mc);
            int rightX = bx + BOX_W - Math.max(
                    textRenderer.getWidth(countStr),
                    textRenderer.getWidth(distStr)) - 8;
            ctx.drawTextWithShadow(textRenderer, Text.literal(countStr),
                    rightX, nameY, diff ? C_TEXT_DIM : C_YELLOW);
            ctx.drawTextWithShadow(textRenderer, Text.literal(distStr),
                    rightX, subY, diff ? C_TEXT_DIM : C_CYAN);

            if (i < visible - 1)
                ctx.fill(bx + 1, rowY + ROW_H - 1, bx + BOX_W - 1, rowY + ROW_H, C_DIVIDER);
        }

        // Scrollbar
        if (results.size() > MAX_VISIBLE) {
            int sbX    = bx + BOX_W - 3;
            int sbH    = areaH;
            int sbY    = ry0;
            int thumbH = Math.max(16, sbH * MAX_VISIBLE / results.size());
            int thumbY = sbY + (sbH - thumbH) * scrollOffset / Math.max(1, results.size() - MAX_VISIBLE);
            ctx.fill(sbX, sbY, sbX + 2, sbY + sbH, 0xFF222222);
            ctx.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, C_CYAN);
        }
    }

    // ── Footer ────────────────────────────────────────────────────────────
    private void renderFooter(DrawContext ctx, int bx, int by, int bh) {
        int fy = by + bh - FOOTER_H;
        ctx.fill(bx + 1, fy, bx + BOX_W - 1, fy + 1, C_DIVIDER);
        ctx.fill(bx + 1, fy + 1, bx + BOX_W - 1, by + bh - 1, C_BG_FOOTER);

        int fx  = bx + 10;
        int ffy = fy + (FOOTER_H - textRenderer.fontHeight) / 2;

        String sel = Text.translatable("stashfinder.search.hint_select").getString();
        String nav = Text.translatable("stashfinder.search.hint_navigate").getString();
        String apl = Text.translatable("stashfinder.search.hint_apply").getString();
        String cls = Text.translatable("stashfinder.search.hint_close").getString();

        if (showHistory && !history.isEmpty()) {
            hint(ctx, fx, ffy, "↑↓", " " + sel + "  ");
            hint(ctx, fx + hw("↑↓ " + sel + "  "), ffy, "Enter", " " + apl + "  ");
        } else {
            hint(ctx, fx, ffy, "↑↓", " " + sel + "  ");
            hint(ctx, fx + hw("↑↓ " + sel + "  "), ffy, "Enter", " " + nav + "  ");
        }

        // Cancel butonu — sağda
        int cbX = bx + BOX_W - 40;
        int cbY = fy + 2;
        int cbW = 34, cbH = FOOTER_H - 4;
        boolean cbHov = mouseX >= cbX && mouseX < cbX + cbW
                     && mouseY >= cbY && mouseY < cbY + cbH;
        ctx.fill(cbX, cbY, cbX + cbW, cbY + cbH, cbHov ? 0xFF3a1a1a : 0xFF2a1a1a);
        int cbBorder = cbHov ? 0xFFAA4444 : 0xFF553333;
        ctx.fill(cbX,          cbY,          cbX + cbW, cbY + 1,       cbBorder);
        ctx.fill(cbX,          cbY + cbH -1, cbX + cbW, cbY + cbH,     cbBorder);
        ctx.fill(cbX,          cbY,          cbX + 1,   cbY + cbH,     cbBorder);
        ctx.fill(cbX + cbW -1, cbY,          cbX + cbW, cbY + cbH,     cbBorder);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(cls),
                cbX + cbW / 2, cbY + (cbH - textRenderer.fontHeight) / 2,
                cbHov ? 0xFFFF8888 : C_RED);
    }

    private void hint(DrawContext ctx, int x, int y, String key, String label) {
        int kw = textRenderer.getWidth(key) + 4;
        ctx.fill(x, y - 1, x + kw, y + textRenderer.fontHeight + 1, 0xFF2a2a2a);
        ctx.fill(x, y - 1, x + kw, y, C_DIVIDER);
        ctx.drawTextWithShadow(textRenderer, Text.literal(key), x + 2, y, C_CYAN);
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), x + kw + 2, y, C_TEXT_DIM);
    }

    private int hw(String s) { return textRenderer.getWidth(s); }

    // ── Klavye ────────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (showSortMenu) { showSortMenu = false; rebuildButtons(); return true; }
            close(); return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            if (!showHistory && !results.isEmpty()) {
                selectedIndex = Math.max(0, selectedIndex - 1);
                if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
            } else if (showHistory) {
                selectedIndex = Math.max(0, selectedIndex - 1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            if (!showHistory && !results.isEmpty()) {
                selectedIndex = Math.min(results.size() - 1, selectedIndex + 1);
                if (selectedIndex >= scrollOffset + MAX_VISIBLE)
                    scrollOffset = selectedIndex - MAX_VISIBLE + 1;
            } else if (showHistory) {
                selectedIndex = Math.min(visibleHistCount() - 1, selectedIndex + 1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (showHistory) applyHistory(selectedIndex);
            else activateSelected();
            return true;
        }
        return super.keyPressed(input);
    }

    private void activateSelected() {
        if (results.isEmpty() || selectedIndex >= results.size()) return;
        String query = searchField != null ? searchField.getText().trim() : "";
        if (!query.isEmpty()) ChestStorage.getInstance().addToHistory(query);
        ChestMemoryHud.setTarget(results.get(selectedIndex).chest);
        close();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hScroll, double vScroll) {
        if (!showHistory && results.size() > MAX_VISIBLE) {
            int maxOff    = results.size() - MAX_VISIBLE;
            scrollOffset  = (int) Math.max(0, Math.min(maxOff, scrollOffset - vScroll));
            rebuildButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hScroll, vScroll);
    }

    @Override public boolean shouldPause() { return false; }

    // ── Yardımcılar ───────────────────────────────────────────────────────
    private void slotBox(DrawContext ctx, int x, int y, int s) {
        ctx.fill(x,     y,     x+s, y+s, C_BG_SLOT);
        ctx.fill(x,     y,     x+s, y+1, C_SLOT_DARK);
        ctx.fill(x,     y,     x+1, y+s, C_SLOT_DARK);
        ctx.fill(x,     y+s-1, x+s, y+s, C_SLOT_LIGHT);
        ctx.fill(x+s-1, y,     x+s, y+s, C_SLOT_LIGHT);
    }

    private ItemStack buildStack(String itemId) {
        try { return Registries.ITEM.get(Identifier.of(itemId)).getDefaultStack(); }
        catch (Exception e) { return ItemStack.EMPTY; }
    }

    private String dirArrow(ChestRecord chest, MinecraftClient mc) {
        if (mc.player == null) return "";
        double angle = (Math.toDegrees(Math.atan2(
                chest.getZ() - mc.player.getZ(),
                chest.getX() - mc.player.getX())) + 360) % 360;
        if (angle < 22.5  || angle >= 337.5) return "→";
        if (angle < 67.5)  return "↘";
        if (angle < 112.5) return "↓";
        if (angle < 157.5) return "↙";
        if (angle < 202.5) return "←";
        if (angle < 247.5) return "↖";
        if (angle < 292.5) return "↑";
        return "↗";
    }
}