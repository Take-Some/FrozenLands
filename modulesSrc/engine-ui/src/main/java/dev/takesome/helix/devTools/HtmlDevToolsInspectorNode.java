package dev.takesome.helix.devTools;

import static dev.takesome.helix.validation.EngineValidator.hasPositiveFiniteSize;

import dev.takesome.helix.devTools.actions.HtmlActionTraceEntry;
import dev.takesome.helix.devTools.css.HtmlCssBoxSnapshot;
import dev.takesome.helix.devTools.css.HtmlCssPropertyCatalog;
import dev.takesome.helix.devTools.css.HtmlCssPropertyDescriptor;
import dev.takesome.helix.devTools.css.HtmlCssPropertySnapshot;
import dev.takesome.helix.devTools.diagnostics.HtmlDiagnosticSnapshot;
import dev.takesome.helix.devTools.dom.HtmlElementSnapshot;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.icons.UiIconRegistries;
import dev.takesome.helix.ui.icons.registry.IconRegistry;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.render.UiRenderContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Engine-ui-rendered F12 DOM/CSS inspector. */
public final class HtmlDevToolsInspectorNode extends Node {
    private static final UiColor DIM = new UiColor(0.0f, 0.0f, 0.0f, 0.18f);
    private static final UiColor SHELL = UiColor.rgba255(248, 250, 252, 250);
    private static final UiColor PANEL = UiColor.rgba255(255, 255, 255, 252);
    private static final UiColor CARD = UiColor.rgba255(248, 250, 252, 248);
    private static final UiColor BORDER = UiColor.rgba255(203, 213, 225, 255);
    private static final UiColor TEXT = UiColor.rgba255(15, 23, 42, 255);
    private static final UiColor MUTED = UiColor.rgba255(71, 85, 105, 255);
    private static final UiColor ACCENT = UiColor.rgba255(8, 145, 178, 255);
    private static final UiColor GOLD = UiColor.rgba255(202, 138, 4, 255);
    private static final UiColor SELECTED = UiColor.rgba255(186, 230, 253, 255);
    private static final UiColor HOVER = UiColor.rgba255(224, 242, 254, 255);
    private static final UiColor BAD = UiColor.rgba255(220, 38, 38, 255);
    private static final UiColor GOOD = UiColor.rgba255(5, 150, 105, 255);
    private static final UiColor DOM_PANEL = UiColor.rgba255(15, 23, 42, 255);
    private static final UiColor DOM_PANEL_DEEP = UiColor.rgba255(2, 6, 23, 255);
    private static final UiColor DOM_BORDER = UiColor.rgba255(30, 41, 59, 255);
    private static final UiColor DOM_TEXT = UiColor.rgba255(226, 232, 240, 255);
    private static final UiColor DOM_MUTED = UiColor.rgba255(148, 163, 184, 255);
    private static final UiColor DOM_ACCENT = UiColor.rgba255(103, 232, 249, 255);
    private static final UiColor DOM_HOVER = UiColor.rgba255(30, 64, 86, 230);
    private static final UiColor DOM_SELECTED = UiColor.rgba255(14, 116, 144, 215);
    private static final UiColor DOM_SCROLL_TRACK = UiColor.rgba255(15, 23, 42, 255);
    private static final UiColor DOM_SCROLL_THUMB = UiColor.rgba255(71, 85, 105, 255);

    private static final String FONT = "engine-ui-system-fs-elliot-pro";
    private static final String FONT_BOLD = "engine-ui-system-fs-elliot-pro-bold";
    private static final String FONT_HEAVY = "engine-ui-system-fs-elliot-pro-heavy";

    private static final int MIN_DOM_ROW_COUNT = 16;
    private static final float TITLEBAR_HEIGHT = 88f;
    private static final int KEY_ENTER = 10;
    private static final int KEY_ESCAPE = 27;
    private static final int KEY_BACKSPACE = 8;
    private static final int KEY_DELETE = 127;
    private static final int KEY_TAB = 9;

    private final HtmlDevToolsController controller = new HtmlDevToolsController();
    private final HtmlCssPropertyCatalog catalog = HtmlCssPropertyCatalog.builtins();
    private final IconRegistry icons = UiIconRegistries.standard();
    private final UiIcon saveIcon = icon("floppy-disk");
    private final UiIcon undoIcon = icon("rotate-left");
    private final UiIcon redoIcon = icon("rotate-right");
    private final UiIcon trashIcon = icon("trash");
    private final UiIcon copyIcon = icon("copy");
    private final UiIcon editIcon = icon("pen");
    private final UiIcon plusIcon = icon("plus");
    private final UiIcon checkIcon = icon("check");
    private final UiIcon xmarkIcon = icon("xmark");
    private final UiIcon codeIcon = icon("code");
    private final UiIcon chevronRightIcon = icon("chevron-right");
    private final UiIcon chevronDownIcon = icon("chevron-down");
    private final ArrayList<ActionHit> actionHits = new ArrayList<>();
    private final Set<String> collapsedNodeKeys = new HashSet<>();
    private final Runnable closeRequested;
    private final boolean externalWindowHost;

    private int hoveredNodeId;
    private String hoveredStyleKey = "";
    private boolean contextMenuOpen;
    private int contextMenuNodeId;
    private float contextMenuX;
    private float contextMenuY;
    private int editingStyleNodeId;
    private String editingStyleName = "";
    private String editingStyleDraftName = "";
    private String editingStyleDraftValue = "";
    private boolean editingStyleValue;
    private boolean addingStyle;
    private String statusMessage = "";
    private UiRect windowRect = new UiRect(0f, 0f, 0f, 0f);
    private UiRect titlebarRect = new UiRect(0f, 0f, 0f, 0f);
    private boolean windowPlaced;
    private boolean draggingWindow;
    private float dragOffsetX;
    private float dragOffsetY;
    private int domScrollOffsetRows;
    private int domScrollMaxRows;
    private UiRect domTreeRect = new UiRect(0f, 0f, 0f, 0f);
    private boolean colorPickerOpen;
    private int colorPickerNodeId;
    private String colorPickerStyleName = "";
    private String colorPickerStyleValue = "";
    private float colorPickerX;
    private float colorPickerY;
    private boolean valueDropdownOpen;
    private int valueDropdownNodeId;
    private String valueDropdownStyleName = "";
    private String valueDropdownStyleValue = "";
    private float valueDropdownX;
    private float valueDropdownY;

    private UiIcon icon(String id) {
        try {
            return icons.require(id);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public HtmlDevToolsInspectorNode() {
        this(null, false);
    }

    public HtmlDevToolsInspectorNode(Runnable closeRequested) {
        this(closeRequested, false);
    }

    public HtmlDevToolsInspectorNode(Runnable closeRequested, boolean externalWindowHost) {
        this.closeRequested = closeRequested;
        this.externalWindowHost = externalWindowHost;
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        HtmlDevToolsSnapshot snapshot = HtmlDevToolsRuntime.remoteSnapshot();
        if (snapshot == null) {
            HtmlDevToolsRuntime.update(HtmlDevToolsRuntime.source());
            snapshot = controller.inspect(HtmlDevToolsRuntime.target());
        }
        UiRect root = absoluteBounds();
        drawSceneHighlight(ctx, snapshot, root);
        drawWindow(ctx, snapshot, root);
    }

    @Override
    protected boolean onInput(UiInputEvent event) {
        if (event == null) return false;
        if (!event.isPointerEvent()) return handleKeyboard(event);
        if (!containsAbsolute(event.mouseX(), event.mouseY())) return false;
        boolean insideWindow = contains(windowRect, event.mouseX(), event.mouseY());
        if (!insideWindow && !draggingWindow) {
            if (hoveredNodeId != 0) {
                hoveredNodeId = 0;
                HtmlDevToolsRuntime.clearHighlight();
                HtmlDevToolsRuntime.dispatchRemoteAction("devtools.node.hover", 0, Map.of());
                markDirty();
            }
            return false;
        }

        if (event.isMouseScroll() && contains(domTreeRect, event.mouseX(), event.mouseY())) {
            int delta = event.scrollY() > 0f ? 3 : -3;
            domScrollOffsetRows = Math.max(0, Math.min(domScrollMaxRows, domScrollOffsetRows + delta));
            event.consume();
            markDirty();
            return true;
        }

        if (event.isMouseMove()) {
            if (draggingWindow) {
                windowRect = new UiRect(event.mouseX() - dragOffsetX, event.mouseY() - dragOffsetY, windowRect.w, windowRect.h);
                event.consume();
                markDirty();
                return true;
            }
            ActionHit hit = actionAt(event.mouseX(), event.mouseY());
            hoveredStyleKey = hit == null ? "" : hit.data().getOrDefault("style-key", "");
            hoveredNodeId = hit == null ? 0 : hit.nodeId();
            HtmlDevToolsRuntime.highlightNode(hoveredNodeId);
            HtmlDevToolsRuntime.dispatchRemoteAction("devtools.node.hover", hoveredNodeId, Map.of());
            markDirty();
            event.consume();
            return true;
        }

        if (event.isMouseDown()) {
            ActionHit hit = actionAt(event.mouseX(), event.mouseY());
            if (event.mouseButton() == UiInputEvent.MouseButton.RIGHT) {
                if (hit != null && hit.nodeId() > 0) {
                    openContextMenu(hit.nodeId(), event.mouseX(), event.mouseY());
                    event.consume();
                    markDirty();
                    return true;
                }
                closeContextMenu();
            }
            if (hit != null) {
                dispatch(hit);
                event.consume();
                markDirty();
                return true;
            }
            closeContextMenu();
            if (!externalWindowHost && contains(titlebarRect, event.mouseX(), event.mouseY())) {
                draggingWindow = true;
                dragOffsetX = event.mouseX() - windowRect.x;
                dragOffsetY = event.mouseY() - windowRect.y;
                event.consume();
                return true;
            }
        }

        if (event.isMouseUp()) {
            draggingWindow = false;
            event.consume();
            markDirty();
            return true;
        }

        if (event.isMouseClick()) {
            // Actions are committed on mouseDown. AWT still emits mouseClicked after
            // release, so dispatching here toggles transient controls twice.
            event.consume();
            markDirty();
            return true;
        }

        event.consume();
        return true;
    }

    private boolean handleKeyboard(UiInputEvent event) {
        if (!editingStyle()) return false;
        if (event.type() == UiInputEvent.Type.TEXT_INPUT) {
            appendEditText(event.text());
            markDirty();
            return true;
        }
        if (event.type() != UiInputEvent.Type.KEY_DOWN) return false;
        int key = event.keyCode();
        if (key == KEY_ESCAPE) {
            cancelStyleEdit();
            markDirty();
            return true;
        }
        if (key == KEY_ENTER) {
            if (!editingStyleValue && !editingStyleDraftName.isBlank()) {
                editingStyleValue = true;
            } else {
                commitStyleEdit();
            }
            markDirty();
            return true;
        }
        if (key == KEY_TAB) {
            editingStyleValue = !editingStyleValue;
            markDirty();
            return true;
        }
        if (key == KEY_BACKSPACE || key == KEY_DELETE) {
            backspaceEditText();
            markDirty();
            return true;
        }
        return true;
    }

    private void drawWindow(UiRenderContext ctx, HtmlDevToolsSnapshot snapshot, UiRect root) {
        actionHits.clear();
        UiRect win = resolveWindowRect(root);
        windowRect = win;

        ctx.fill(win, SHELL);
        ctx.stroke(win, BORDER, 1f);

        float headerY = externalWindowHost ? win.y + win.h - TITLEBAR_HEIGHT : win.y;
        float statusY = externalWindowHost ? win.y : win.y + win.h - 32f;
        float contentY = externalWindowHost ? win.y + 40f : win.y + 74f;
        float contentH = externalWindowHost ? win.h - TITLEBAR_HEIGHT - 50f : win.h - 112f;

        header(ctx, snapshot, win.x, headerY, win.w);
        status(ctx, snapshot, win.x, statusY, win.w);

        float leftW = Math.min(560f, Math.max(460f, win.w * 0.44f));
        float rightW = win.w - leftW - 38f;
        domTree(ctx, snapshot, win.x + 14f, contentY, leftW, contentH);
        inspector(ctx, snapshot, win.x + leftW + 28f, contentY, rightW, contentH);
        contextMenu(ctx);
        valueDropdown(ctx);
        colorPicker(ctx);
    }

    private UiRect resolveWindowRect(UiRect root) {
        if (externalWindowHost) {
            windowPlaced = true;
            return new UiRect(root.x, root.y, root.w, root.h);
        }
        float w = Math.max(920f, root.w - Math.max(48f, root.w * 0.10f));
        float h = Math.max(560f, root.h - Math.max(48f, root.h * 0.10f));
        if (w > root.w - 24f) w = root.w - 24f;
        if (h > root.h - 24f) h = root.h - 24f;
        if (!windowPlaced || windowRect.w <= 0f || windowRect.h <= 0f) {
            windowPlaced = true;
            return new UiRect(root.x + (root.w - w) * 0.5f, root.y + (root.h - h) * 0.5f, w, h);
        }
        float x = clamp(windowRect.x, root.x + 12f, root.x + root.w - w - 12f);
        float y = clamp(windowRect.y, root.y + 12f, root.y + root.h - h - 12f);
        return new UiRect(x, y, w, h);
    }

    private void header(UiRenderContext ctx, HtmlDevToolsSnapshot snapshot, float x, float y, float w) {
        titlebarRect = new UiRect(x, y, w, TITLEBAR_HEIGHT);
        ctx.fill(titlebarRect, PANEL);
        ctx.fill(new UiRect(x, yFromTop(y, TITLEBAR_HEIGHT, 0f, 48f), w, 48f), UiColor.rgba255(255, 255, 255, 255));
        ctx.stroke(new UiRect(x, yFromTop(y, TITLEBAR_HEIGHT, 47f, 1f), w, 1f), BORDER, 1f);
        ctx.stroke(new UiRect(x, y + TITLEBAR_HEIGHT - 1f, w, 1f), BORDER, 1f);

        float titleY = yFromTop(y, TITLEBAR_HEIGHT, 8f, 18f);
        float subtitleY = yFromTop(y, TITLEBAR_HEIGHT, 27f, 15f);
        text(ctx, "HELIX ENGINE UI", x + 20f, titleY, Math.max(160f, w - 330f), 18f, 0.78f, ACCENT, FONT_BOLD);
        text(ctx, statusMessage.isBlank() ? "F12 Current Document Inspector" : statusMessage,
                x + 20f, subtitleY, Math.max(160f, w - 330f), 15f, 0.56f, MUTED, FONT);

        float actionY = yFromTop(y, TITLEBAR_HEIGHT, 10f, 32f);
        iconButton(ctx, undoIcon, "", "devtools.undo", Map.of(), x + w - 282f, actionY, 32f, snapshot.canUndo());
        iconButton(ctx, redoIcon, "", "devtools.redo", Map.of(), x + w - 244f, actionY, 32f, snapshot.canRedo());
        iconButton(ctx, saveIcon, "Save", "devtools.save", Map.of(), x + w - 204f, actionY, 78f, snapshot.dirty());
        button(ctx, "Pick", "devtools.pick.toggle", Map.of(), x + w - 118f, actionY, 58f, snapshot.session().pickerEnabled());
        iconButton(ctx, xmarkIcon, "", "devtools.close", Map.of(), x + w - 52f, actionY, 32f, true);

        float tabY = yFromTop(y, TITLEBAR_HEIGHT, 54f, 28f);
        float tabX = x + 16f;
        tabX = tab(ctx, "Elements", HtmlDevToolsTab.ELEMENTS, snapshot, tabX, tabY, 86f);
        tabX = tab(ctx, "Styles", HtmlDevToolsTab.STYLES, snapshot, tabX + 8f, tabY, 72f);
        tabX = tab(ctx, "Computed", HtmlDevToolsTab.COMPUTED, snapshot, tabX + 8f, tabY, 96f);
        tabX = tab(ctx, "Layout", HtmlDevToolsTab.LAYOUT, snapshot, tabX + 8f, tabY, 76f);
        tabX = tab(ctx, "Source", HtmlDevToolsTab.SOURCE, snapshot, tabX + 8f, tabY, 76f);
        tab(ctx, "Diagnostics", HtmlDevToolsTab.DIAGNOSTICS, snapshot, tabX + 8f, tabY, 112f);
    }

    private void dragger(UiRenderContext ctx, float x, float y) {
        for (int i = 0; i < 3; i++) {
            ctx.stroke(new UiRect(x + i * 6f, y, 1f, 10f), MUTED, 1f);
            ctx.stroke(new UiRect(x + i * 6f + 2f, y, 1f, 10f), MUTED, 1f);
        }
    }

    private float tab(UiRenderContext ctx, String label, HtmlDevToolsTab tab, HtmlDevToolsSnapshot snapshot, float x, float y, float w) {
        boolean active = snapshot.session().selectedTab() == tab;
        UiRect rect = new UiRect(x, y, w, 26f);
        ctx.fill(rect, active ? SELECTED : CARD);
        ctx.stroke(rect, active ? ACCENT : BORDER, 1f);
        text(ctx, label, rect.x, rect.y + 6f, rect.w, 14f, 0.56f, active ? TEXT : TEXT, FONT_BOLD, TextAlign.CENTER);
        actionHits.add(new ActionHit("devtools.tab.select", Map.of("tab", tab.id()), 0, rect));
        return x + w;
    }

    private void button(UiRenderContext ctx, String label, String actionId, Map<String, String> data, float x, float y, float w, boolean active) {
        UiRect rect = new UiRect(x, y, w, 28f);
        ctx.fill(rect, active ? SELECTED : CARD);
        ctx.stroke(rect, active ? ACCENT : BORDER, 1f);
        text(ctx, label, x, y + 7f, w, 13f, 0.56f, active ? TEXT : TEXT, FONT_BOLD, TextAlign.CENTER);
        actionHits.add(new ActionHit(actionId, data, 0, rect));
    }

    private void iconButton(UiRenderContext ctx, UiIcon icon, String label, String actionId, Map<String, String> data, float x, float y, float w, boolean active) {
        UiRect rect = new UiRect(x, y, w, 32f);
        UiColor ink = active ? ACCENT : MUTED;
        ctx.fill(rect, active ? SELECTED : CARD);
        ctx.stroke(rect, active ? ACCENT : BORDER, 1f);
        String safeLabel = label == null ? "" : label.trim();
        if (safeLabel.isBlank()) {
            if (icon != null && ctx.supportsIcons()) ctx.icon(icon, new UiRect(x + (w - 15f) * 0.5f, y + 8f, 15f, 15f), 0.72f, ink, TextAlign.CENTER);
        } else {
            float labelX = x;
            float labelW = w;
            if (icon != null && ctx.supportsIcons()) {
                ctx.icon(icon, new UiRect(x + 10f, y + 8f, 14f, 14f), 0.70f, ink, TextAlign.CENTER);
                labelX = x + 26f;
                labelW = w - 28f;
            }
            text(ctx, safeLabel, labelX, y + 9f, labelW, 13f, 0.56f, active ? TEXT : MUTED, FONT_BOLD, TextAlign.CENTER);
        }
        actionHits.add(new ActionHit(actionId, data, 0, rect));
    }

    private void domTree(UiRenderContext ctx, HtmlDevToolsSnapshot snapshot, float x, float y, float w, float h) {
        domTreeRect = new UiRect(x, y, w, h);
        ctx.fill(domTreeRect, DOM_PANEL);
        ctx.fill(new UiRect(x + 1f, y + 1f, Math.max(0f, w - 2f), Math.max(0f, h - 2f)), DOM_PANEL_DEEP);
        ctx.stroke(domTreeRect, DOM_BORDER, 1f);
        ctx.stroke(new UiRect(x + 1f, yFromTop(y, h, 58f, 1f), Math.max(0f, w - 2f), 1f), DOM_BORDER, 1f);
        text(ctx, "Current Document Structure", x + 14f, yFromTop(y, h, 10f, 24f), w - 32f, 24f, 0.88f, DOM_TEXT, FONT_BOLD);
        text(ctx, "HTML tree · hover row = highlight game UI box", x + 14f, yFromTop(y, h, 38f, 20f), w - 32f, 20f, 0.66f, DOM_MUTED, FONT);

        if (snapshot.elements().isEmpty()) {
            domScrollOffsetRows = 0;
            domScrollMaxRows = 0;
            text(ctx, "No document nodes captured yet", x + 14f, yFromTop(y, h, 64f, 18f), w - 32f, 18f, 0.60f, BAD, FONT_BOLD);
            return;
        }

        List<HtmlElementSnapshot> visible = visibleElements(snapshot);
        int selected = snapshot.session().selectedNodeId();
        int count = 0;
        int rowLimit = Math.max(MIN_DOM_ROW_COUNT, (int) ((h - 112f) / 28f));
        domScrollMaxRows = Math.max(0, visible.size() - rowLimit);
        domScrollOffsetRows = Math.max(0, Math.min(domScrollOffsetRows, domScrollMaxRows));
        float rowTop = 68f;
        for (int i = domScrollOffsetRows; i < visible.size(); i++) {
            if (count >= rowLimit) break;
            drawDomRow(ctx, visible.get(i), selected, x + 8f, yFromTop(y, h, rowTop, 26f), w - 28f, 26f);
            rowTop += 28f;
            count++;
        }
        drawDomScrollbar(ctx, x, y, w, h, visible.size(), rowLimit);
        text(ctx, snapshot.elements().size() + " nodes · " + count + " visible", x + 14f, y + 6f, w - 32f, 20f, 0.64f, DOM_MUTED, FONT);
    }

    private void drawDomScrollbar(UiRenderContext ctx, float x, float y, float w, float h, int totalRows, int visibleRows) {
        if (totalRows <= visibleRows || domScrollMaxRows <= 0) return;
        float trackH = Math.max(40f, h - 104f);
        UiRect track = new UiRect(x + w - 10f, y + 34f, 4f, trackH);
        ctx.fill(track, DOM_SCROLL_TRACK);
        float thumbH = Math.max(28f, track.h * Math.min(1f, Math.max(0.05f, visibleRows / (float)Math.max(1, totalRows))));
        float progress = domScrollMaxRows <= 0 ? 0f : domScrollOffsetRows / (float)domScrollMaxRows;
        float thumbY = track.y + (track.h - thumbH) * (1f - progress);
        UiRect thumb = new UiRect(track.x, thumbY, track.w, thumbH);
        ctx.fill(thumb, DOM_SCROLL_THUMB);
        ctx.stroke(thumb, DOM_ACCENT, 1f);
    }

    private void drawDomRow(UiRenderContext ctx, HtmlElementSnapshot element, int selected, float x, float y, float w, float h) {
        boolean active = element.id() == selected;
        boolean hover = element.id() == hoveredNodeId;
        UiRect row = new UiRect(x, y, w, h);
        if (active) ctx.fill(row, DOM_SELECTED);
        else if (hover) ctx.fill(row, DOM_HOVER);
        if (hover || active) ctx.stroke(row, DOM_ACCENT, 1f);
        actionHits.add(new ActionHit("devtools.node.select", Map.of("node-id", String.valueOf(element.id())), element.id(), row));

        float dimW = 128f;
        float displayW = 78f;
        float selectedW = active ? 62f : 0f;
        float dimX = x + w - dimW - 8f;
        float displayX = dimX - displayW - 8f;
        float selectedX = displayX - selectedW - (active ? 8f : 0f);
        float textRight = (active ? selectedX : displayX) - 8f;

        float indent = Math.min(176f, element.depth() * 14f);
        float cursor = x + 8f + indent;
        if (element.hasChildren()) {
            UiRect toggle = new UiRect(cursor, y + 4f, 18f, 18f);
            String collapseKey = collapseKey(element);
            UiIcon toggleIcon = collapsedNodeKeys.contains(collapseKey) ? chevronRightIcon : chevronDownIcon;
            if (toggleIcon != null && ctx.supportsIcons()) ctx.icon(toggleIcon, toggle, 0.62f, DOM_MUTED, TextAlign.CENTER);
            actionHits.add(new ActionHit("devtools.node.toggle", Map.of("node-id", String.valueOf(element.id()), "collapse-key", collapseKey), element.id(), toggle));
        } else if (codeIcon != null && ctx.supportsIcons()) {
            ctx.icon(codeIcon, new UiRect(cursor + 2f, y + 6f, 14f, 14f), 0.46f, DOM_MUTED, TextAlign.CENTER);
        }
        cursor += 24f;

        String markup = openTagMarkup(element);
        int maxMarkupChars = Math.max(1, (int) ((textRight - cursor) / 7.2f));
        text(ctx, truncate(markup, maxMarkupChars), cursor, y + 5f, Math.max(0f, textRight - cursor), 18f, 0.70f, DOM_ACCENT, FONT_BOLD);

        String display = element.display().toLowerCase(Locale.ROOT);
        if (!display.isBlank() && !"block".equals(display)) badge(ctx, truncate(display, 9), displayX, y + 4f, displayColor(display));
        if (active) badge(ctx, "==$0", selectedX, y + 4f, GOLD);
        text(ctx, element.dimensionsLabel(), dimX, y + 5f, dimW, 18f, 0.58f, DOM_MUTED, FONT, TextAlign.RIGHT);

    }

    private float yFromTop(float containerY, float containerH, float top, float height) {
        return containerY + containerH - top - height;
    }

    private String openTagMarkup(HtmlElementSnapshot element) {
        if (element == null) return "";
        String attrs = element.attributeText();
        return attrs == null || attrs.isBlank() ? "<" + element.tag() + ">" : "<" + element.tag() + " " + attrs + ">";
    }

    private void inspector(UiRenderContext ctx, HtmlDevToolsSnapshot snapshot, float x, float y, float w, float h) {
        HtmlElementSnapshot selected = snapshot.selectedElement();
        ctx.fill(new UiRect(x, y, w, h), PANEL);
        ctx.stroke(new UiRect(x, y, w, h), BORDER, 1f);
        text(ctx, selected == null ? "No selected node" : selected.path(), x + 14f, y + 10f, w - 28f, 20f, 0.64f, GOLD, FONT_BOLD);

        HtmlDevToolsTab tab = snapshot.session().selectedTab();
        if (tab == HtmlDevToolsTab.STYLES) {
            styleTable(ctx, snapshot, "Matched Styles", snapshot.styles(), x + 14f, y + 44f, w - 28f, h - 62f, rows(h - 62f));
        } else if (tab == HtmlDevToolsTab.COMPUTED) {
            table(ctx, "Computed Style", snapshot.computed(), x + 14f, y + 44f, w - 28f, h - 62f, rows(h - 62f));
        } else if (tab == HtmlDevToolsTab.LAYOUT) {
            layoutPanel(ctx, selected, snapshot.layout(), x + 14f, y + 44f, w - 28f, h - 62f);
        } else if (tab == HtmlDevToolsTab.SOURCE) {
            sourcePanel(ctx, snapshot.code(), x + 14f, y + 44f, w - 28f, h - 62f);
        } else if (tab == HtmlDevToolsTab.DIAGNOSTICS) {
            diagnosticsPanel(ctx, snapshot, x + 14f, y + 44f, w - 28f, h - 62f);
        } else {
            float stylesW = w * 0.52f;
            styleTable(ctx, snapshot, "Styles", snapshot.styles(), x + 14f, y + 44f, stylesW - 20f, h - 72f, 14);
            table(ctx, "Computed", snapshot.computed(), x + stylesW + 6f, y + 44f, w - stylesW - 20f, h * 0.50f, 11);
            layout(ctx, snapshot.layout(), x + stylesW + 6f, y + 60f + h * 0.50f, w - stylesW - 20f, 110f);
            registry(ctx, x + stylesW + 6f, y + h - 112f, w - stylesW - 20f, 96f);
        }
    }

    private void table(UiRenderContext ctx, String title, List<HtmlCssPropertySnapshot> rows, float x, float y, float w, float h, int limit) {
        ctx.fill(new UiRect(x, y, w, h), CARD);
        ctx.stroke(new UiRect(x, y, w, h), BORDER, 1f);
        text(ctx, title, x + 10f, y + 8f, w - 20f, 18f, 0.68f, TEXT, FONT_BOLD);
        float rowY = y + 34f;
        int count = 0;
        if (rows != null) {
            for (HtmlCssPropertySnapshot row : rows) {
                if (count >= limit || rowY > y + h - 20f) break;
                UiColor keyColor = row.origin().startsWith("invalid") ? BAD : ACCENT;
                text(ctx, row.name(), x + 10f, rowY, w * 0.36f, 16f, 0.56f, keyColor, FONT_BOLD);
                text(ctx, row.value(), x + w * 0.38f, rowY, w * 0.40f, 16f, 0.56f, TEXT, FONT);
                text(ctx, row.origin(), x + w * 0.80f, rowY, w * 0.18f, 16f, 0.48f, MUTED, FONT);
                rowY += 18f;
                count++;
            }
        }
        if (count == 0) text(ctx, "No style rows", x + 10f, y + 34f, w - 20f, 16f, 0.56f, MUTED, FONT);
    }


    private void styleTable(UiRenderContext ctx, HtmlDevToolsSnapshot snapshot, String title, List<HtmlCssPropertySnapshot> rows, float x, float y, float w, float h, int limit) {
        ctx.fill(new UiRect(x, y, w, h), CARD);
        ctx.stroke(new UiRect(x, y, w, h), BORDER, 1f);
        text(ctx, title, x + 12f, y + 8f, w - 24f, 22f, 0.82f, TEXT, FONT_BOLD);
        HtmlElementSnapshot selected = snapshot == null ? null : snapshot.selectedElement();
        int nodeId = selected == null ? 0 : selected.id();
        float rowY = y + 38f;
        int count = 0;
        if (rows != null) {
            for (HtmlCssPropertySnapshot row : rows) {
                if (count >= limit || rowY > y + h - 52f) break;
                drawStyleRow(ctx, nodeId, row, x + 8f, rowY, w - 16f, 24f);
                rowY += 26f;
                count++;
            }
        }
        if (addingStyle && editingStyleNodeId == nodeId && rowY <= y + h - 52f) {
            drawDraftStyleRow(ctx, nodeId, x + 8f, rowY, w - 16f, 24f);
            rowY += 26f;
        }
        UiRect add = new UiRect(x + 8f, y + h - 34f, w - 16f, 26f);
        ctx.fill(add, HOVER);
        ctx.stroke(add, BORDER, 1f);
        if (plusIcon != null && ctx.supportsIcons()) ctx.icon(plusIcon, new UiRect(add.x + 9f, add.y + 7f, 12f, 12f), 0.62f, ACCENT, TextAlign.CENTER);
        text(ctx, "add property", add.x + 28f, add.y + 6f, add.w - 36f, 14f, 0.66f, ACCENT, FONT_BOLD);
        actionHits.add(new ActionHit("devtools.style.add", Map.of("node-id", String.valueOf(nodeId)), nodeId, add));
        if (count == 0 && !addingStyle) text(ctx, "No editable style rows", x + 12f, y + 38f, w - 24f, 18f, 0.66f, MUTED, FONT);
    }

    private void drawStyleRow(UiRenderContext ctx, int nodeId, HtmlCssPropertySnapshot row, float x, float y, float w, float h) {
        String key = styleKey(nodeId, row.name());
        boolean hover = key.equals(hoveredStyleKey);
        boolean disabled = row.disabled();
        UiRect rowRect = new UiRect(x, y, w, h);
        if (hover || disabled) ctx.fill(rowRect, disabled ? UiColor.rgba255(241, 245, 249, 255) : HOVER);
        UiColor keyColor = disabled ? MUTED : (row.origin().startsWith("invalid") ? BAD : ACCENT);
        UiColor valueColor = disabled ? MUTED : TEXT;
        UiRect check = new UiRect(x + 4f, y + 4f, 16f, 16f);
        ctx.fill(check, PANEL);
        ctx.stroke(check, disabled ? MUTED : GOOD, 1f);
        if (!disabled && checkIcon != null && ctx.supportsIcons()) ctx.icon(checkIcon, check, 0.62f, GOOD, TextAlign.CENTER);
        Map<String, String> actionData = Map.of("style-name", row.name(), "style-value", row.value(), "style-key", key);
        actionHits.add(new ActionHit("devtools.style.toggle", actionData, nodeId, check));

        boolean editing = editingStyleNodeId == nodeId && editingStyleName.equals(row.name()) && !addingStyle;
        String name = editing ? editingStyleDraftName : row.name();
        String value = editing ? editingStyleDraftValue : row.value();
        UiRect nameRect = new UiRect(x + 28f, y + 3f, w * 0.34f, 19f);
        UiRect valueRect = new UiRect(x + w * 0.40f, y + 3f, w * 0.36f, 19f);
        text(ctx, name, nameRect.x, nameRect.y, nameRect.w, nameRect.h, 0.78f, keyColor, FONT_BOLD);
        float valueTextX = valueRect.x;
        float valueTextW = valueRect.w;
        if (isColorProperty(row.name())) {
            UiRect swatch = new UiRect(valueRect.x, y + 5f, 14f, 14f);
            drawColorSwatch(ctx, swatch, value);
            actionHits.add(new ActionHit("devtools.color.open", actionData, nodeId, swatch));
            valueTextX += 20f;
            valueTextW = Math.max(0f, valueTextW - 20f);
        }
        if (dropdownOptions(row.name()).size() > 0) {
            UiRect drop = new UiRect(valueRect.x + valueRect.w - 16f, y + 5f, 14f, 14f);
            if (chevronDownIcon != null && ctx.supportsIcons()) ctx.icon(chevronDownIcon, drop, 0.54f, MUTED, TextAlign.CENTER);
            actionHits.add(new ActionHit("devtools.value.dropdown", actionData, nodeId, valueRect));
            valueTextW = Math.max(0f, valueTextW - 18f);
        }
        text(ctx, value, valueTextX, valueRect.y, valueTextW, valueRect.h, 0.72f, valueColor, FONT);
        text(ctx, row.origin(), x + w * 0.80f, y + 4f, w * 0.18f, 18f, 0.58f, MUTED, FONT);
        if (editing) drawEditCaret(ctx, editingStyleValue ? valueRect : nameRect);

        actionHits.add(new ActionHit("devtools.style.toggle", actionData, nodeId, nameRect));
        if (!isColorProperty(row.name()) && dropdownOptions(row.name()).isEmpty()) actionHits.add(new ActionHit("devtools.style.edit.value", actionData, nodeId, valueRect));
    }

    private void drawDraftStyleRow(UiRenderContext ctx, int nodeId, float x, float y, float w, float h) {
        ctx.fill(new UiRect(x, y, w, h), HOVER);
        String name = editingStyleDraftName;
        String value = editingStyleDraftValue;
        UiRect nameRect = new UiRect(x + 28f, y + 3f, w * 0.34f, 19f);
        UiRect valueRect = new UiRect(x + w * 0.40f, y + 3f, w * 0.36f, 19f);
        text(ctx, name.isBlank() ? "property" : name, nameRect.x, nameRect.y, nameRect.w, nameRect.h, 0.74f, ACCENT, FONT_BOLD);
        text(ctx, value.isBlank() ? "value" : value, valueRect.x, valueRect.y, valueRect.w, valueRect.h, 0.72f, TEXT, FONT);
        drawEditCaret(ctx, editingStyleValue ? valueRect : nameRect);
    }

    private void drawEditCaret(UiRenderContext ctx, UiRect field) {
        if (ctx == null || field == null) return;
        ctx.fill(new UiRect(field.x + Math.max(8f, field.w - 6f), field.y + 2f, 2f, Math.max(10f, field.h - 4f)), ACCENT);
    }

    private void layout(UiRenderContext ctx, HtmlCssBoxSnapshot box, float x, float y, float w, float h) {
        HtmlCssBoxSnapshot safe = box == null ? HtmlCssBoxSnapshot.empty() : box;
        ctx.fill(new UiRect(x, y, w, h), CARD);
        ctx.stroke(new UiRect(x, y, w, h), BORDER, 1f);
        text(ctx, "Layout", x + 10f, y + 8f, w - 20f, 18f, 0.68f, TEXT, FONT_BOLD);
        text(ctx, "x " + Math.round(safe.x()) + "   y " + Math.round(safe.y()), x + 10f, y + 34f, w - 20f, 16f, 0.58f, MUTED, FONT);
        text(ctx, "w " + Math.round(safe.width()) + "   h " + Math.round(safe.height()), x + 10f, y + 54f, w - 20f, 16f, 0.58f, MUTED, FONT);
    }

    private void layoutPanel(UiRenderContext ctx, HtmlElementSnapshot selected, HtmlCssBoxSnapshot box, float x, float y, float w, float h) {
        ctx.fill(new UiRect(x, y, w, h), CARD);
        ctx.stroke(new UiRect(x, y, w, h), BORDER, 1f);
        HtmlCssBoxSnapshot safe = box == null ? HtmlCssBoxSnapshot.empty() : box;
        text(ctx, "Box Model", x + 12f, y + 10f, w - 24f, 22f, 0.72f, TEXT, FONT_BOLD);
        String target = selected == null ? "no selected node" : selected.selector();
        text(ctx, target, x + 12f, y + 34f, w - 24f, 18f, 0.58f, GOLD, FONT);
        float cx = x + 42f;
        float cy = y + 82f;
        boxLayer(ctx, "margin", cx, cy, w - 84f, 210f, UiColor.rgba255(40, 52, 72, 210));
        boxLayer(ctx, "border", cx + 26f, cy + 30f, w - 136f, 150f, UiColor.rgba255(64, 95, 120, 220));
        boxLayer(ctx, "padding", cx + 52f, cy + 58f, w - 188f, 94f, UiColor.rgba255(18, 85, 105, 230));
        boxLayer(ctx, Math.round(safe.width()) + " × " + Math.round(safe.height()), cx + 84f, cy + 82f, w - 252f, 44f, UiColor.rgba255(10, 20, 30, 245));
        text(ctx, "x=" + Math.round(safe.x()) + "  y=" + Math.round(safe.y()) + "  width=" + Math.round(safe.width()) + "  height=" + Math.round(safe.height()),
                x + 12f, y + h - 36f, w - 24f, 18f, 0.62f, MUTED, FONT);
    }

    private void boxLayer(UiRenderContext ctx, String label, float x, float y, float w, float h, UiColor color) {
        UiRect rect = new UiRect(x, y, w, h);
        ctx.fill(rect, color);
        ctx.stroke(rect, BORDER, 1f);
        text(ctx, label, x, y + 8f, w, 16f, 0.58f, TEXT, FONT_BOLD, TextAlign.CENTER);
    }

    private void sourcePanel(UiRenderContext ctx, HtmlCodeView code, float x, float y, float w, float h) {
        HtmlCodeView safe = code == null ? HtmlCodeView.empty() : code;
        ctx.fill(new UiRect(x, y, w, h), CARD);
        ctx.stroke(new UiRect(x, y, w, h), BORDER, 1f);
        text(ctx, safe.path().isBlank() ? "Source" : safe.path(), x + 12f, y + 10f, w - 24f, 20f, 0.68f, TEXT, FONT_BOLD);
        String[] lines = safe.text().isBlank() ? new String[]{"No source text captured yet."} : safe.text().split("\\R", -1);
        int max = Math.min(lines.length, rows(h - 44f));
        float rowY = y + 38f;
        for (int i = 0; i < max; i++) {
            String prefix = String.format(Locale.ROOT, "%02d", i + 1);
            text(ctx, prefix, x + 12f, rowY, 28f, 16f, 0.50f, MUTED, FONT, TextAlign.RIGHT);
            text(ctx, truncate(lines[i], (int) ((w - 56f) / 6.2f)), x + 48f, rowY, w - 60f, 16f, 0.54f, TEXT, FONT);
            rowY += 17f;
        }
    }

    private void diagnosticsPanel(UiRenderContext ctx, HtmlDevToolsSnapshot snapshot, float x, float y, float w, float h) {
        ctx.fill(new UiRect(x, y, w, h), CARD);
        ctx.stroke(new UiRect(x, y, w, h), BORDER, 1f);
        text(ctx, "Diagnostics", x + 12f, y + 10f, w - 24f, 20f, 0.68f, TEXT, FONT_BOLD);
        float rowY = y + 38f;
        int max = rows(h - 48f);
        int count = 0;
        for (HtmlDiagnosticSnapshot diagnostic : snapshot.diagnostics()) {
            if (count >= max) break;
            UiColor color = "error".equalsIgnoreCase(diagnostic.severity()) ? BAD : GOLD;
            text(ctx, diagnostic.severity(), x + 12f, rowY, 56f, 16f, 0.52f, color, FONT_BOLD);
            text(ctx, diagnostic.code(), x + 74f, rowY, 160f, 16f, 0.52f, ACCENT, FONT);
            text(ctx, truncate(diagnostic.message(), (int) ((w - 252f) / 6.2f)), x + 240f, rowY, w - 252f, 16f, 0.52f, TEXT, FONT);
            rowY += 18f;
            count++;
        }
        for (HtmlActionTraceEntry action : snapshot.actions()) {
            if (count >= max) break;
            text(ctx, "action", x + 12f, rowY, 56f, 16f, 0.52f, GOOD, FONT_BOLD);
            text(ctx, action.actionId(), x + 74f, rowY, w - 86f, 16f, 0.52f, TEXT, FONT);
            rowY += 18f;
            count++;
        }
        if (count == 0) text(ctx, "No diagnostics or action trace entries", x + 12f, rowY, w - 24f, 16f, 0.56f, MUTED, FONT);
    }

    private void registry(UiRenderContext ctx, float x, float y, float w, float h) {
        ctx.fill(new UiRect(x, y, w, h), CARD);
        ctx.stroke(new UiRect(x, y, w, h), BORDER, 1f);
        text(ctx, "CSS Registry", x + 10f, y + 8f, w - 20f, 18f, 0.68f, TEXT, FONT_BOLD);
        int count = 0;
        float rowY = y + 34f;
        for (HtmlCssPropertyDescriptor d : catalog.definitions()) {
            if (count >= 4) break;
            text(ctx, d.name() + " = " + d.initialValue(), x + 10f, rowY, w - 20f, 15f, 0.50f, MUTED, FONT);
            rowY += 15f;
            count++;
        }
    }

    private void status(UiRenderContext ctx, HtmlDevToolsSnapshot snapshot, float x, float y, float w) {
        ctx.fill(new UiRect(x, y, w, 32f), UiColor.rgba255(241, 245, 249, 255));
        String hover = hoveredNodeId > 0 ? " · hover #" + hoveredNodeId : "";
        text(ctx, "F12 inspector · " + snapshot.session().selectedTab().id() + " · live DOM " + snapshot.elements().size() + " · CSS properties " + catalog.definitions().size() + hover,
                x + 14f, y + 8f, w - 28f, 18f, 0.58f, MUTED, FONT);
    }

    private void drawSceneHighlight(UiRenderContext ctx, HtmlDevToolsSnapshot snapshot, UiRect root) {
        if (externalWindowHost) return;
        HtmlElementSnapshot element = elementById(snapshot, hoveredNodeId > 0 ? hoveredNodeId : snapshot.session().selectedNodeId());
        if (element == null || !hasPositiveFiniteSize(element.width(), element.height())) return;
        UiRect rect = new UiRect(element.x(), element.y(), element.width(), element.height());
        ctx.fill(rect, new UiColor(0.0f, 0.85f, 1.0f, 0.16f));
        ctx.stroke(rect, ACCENT, 2f);
        String label = element.selector() + "  " + element.dimensionsLabel();
        float labelW = Math.min(root.w - 32f, Math.max(170f, label.length() * 6.8f + 18f));
        float labelX = clamp(rect.x, root.x + 12f, root.x + root.w - labelW - 12f);
        float labelY = rect.y - 26f < root.y + 8f ? rect.y + rect.h + 8f : rect.y - 26f;
        UiRect labelRect = new UiRect(labelX, clamp(labelY, root.y + 8f, root.y + root.h - 30f), labelW, 22f);
        ctx.fill(labelRect, UiColor.rgba255(248, 250, 252, 245));
        ctx.stroke(labelRect, ACCENT, 1f);
        text(ctx, truncate(label, (int) ((labelW - 14f) / 6.2f)), labelRect.x + 7f, labelRect.y + 5f, labelRect.w - 14f, 12f, 0.52f, TEXT, FONT_BOLD);
    }

    private void openContextMenu(int nodeId, float x, float y) {
        contextMenuOpen = true;
        contextMenuNodeId = Math.max(0, nodeId);
        contextMenuX = x;
        contextMenuY = y;
    }

    private void closeContextMenu() {
        contextMenuOpen = false;
        contextMenuNodeId = 0;
    }


    private void colorPicker(UiRenderContext ctx) {
        if (!colorPickerOpen || colorPickerNodeId <= 0 || colorPickerStyleName.isBlank()) return;
        String[] presets = new String[] {
                "transparent", "#ffffff", "#000000", "#94a3b8",
                "#ef4444", "#f97316", "#eab308", "#22c55e",
                "#06b6d4", "#3b82f6", "#8b5cf6", "#ec4899"
        };
        float w = 188f;
        float h = 92f;
        float x = clamp(colorPickerX, windowRect.x + 8f, windowRect.x + windowRect.w - w - 8f);
        float y = clamp(colorPickerY - h, windowRect.y + 8f, windowRect.y + windowRect.h - h - 8f);
        UiRect rect = new UiRect(x, y, w, h);
        ctx.fill(rect, PANEL);
        ctx.stroke(rect, ACCENT, 1f);
        text(ctx, colorPickerStyleName, x + 10f, y + h - 22f, w - 20f, 14f, 0.56f, TEXT, FONT_BOLD);
        for (int i = 0; i < presets.length; i++) {
            float sx = x + 10f + (i % 6) * 28f;
            float sy = y + 12f + (1 - i / 6) * 26f;
            UiRect swatch = new UiRect(sx, sy, 20f, 20f);
            drawColorSwatch(ctx, swatch, presets[i]);
            Map<String, String> data = Map.of("style-name", colorPickerStyleName, "style-value", presets[i]);
            actionHits.add(new ActionHit("devtools.color.pick", data, colorPickerNodeId, swatch));
        }
    }

    private void valueDropdown(UiRenderContext ctx) {
        if (!valueDropdownOpen || valueDropdownNodeId <= 0 || valueDropdownStyleName.isBlank()) return;
        List<String> options = dropdownOptions(valueDropdownStyleName);
        if (options.isEmpty()) return;
        float w = 178f;
        float rowH = 24f;
        float h = Math.min(220f, 12f + options.size() * rowH);
        float x = clamp(valueDropdownX, windowRect.x + 8f, windowRect.x + windowRect.w - w - 8f);
        float y = clamp(valueDropdownY - h, windowRect.y + 8f, windowRect.y + windowRect.h - h - 8f);
        UiRect rect = new UiRect(x, y, w, h);
        ctx.fill(rect, PANEL);
        ctx.stroke(rect, ACCENT, 1f);
        int max = Math.min(options.size(), Math.max(1, (int)((h - 10f) / rowH)));
        for (int i = 0; i < max; i++) {
            String option = options.get(i);
            UiRect item = new UiRect(x + 6f, y + h - 8f - rowH * (i + 1), w - 12f, rowH - 2f);
            boolean active = option.equalsIgnoreCase(valueDropdownStyleValue == null ? "" : valueDropdownStyleValue.trim());
            ctx.fill(item, active ? SELECTED : CARD);
            ctx.stroke(item, active ? ACCENT : BORDER, 1f);
            text(ctx, option, item.x + 8f, item.y + 6f, item.w - 16f, 13f, 0.58f, active ? TEXT : MUTED, FONT_BOLD);
            Map<String, String> data = Map.of("style-name", valueDropdownStyleName, "style-value", option);
            actionHits.add(new ActionHit("devtools.value.option", data, valueDropdownNodeId, item));
        }
    }

    private void openColorPicker(ActionHit hit) {
        colorPickerOpen = true;
        valueDropdownOpen = false;
        colorPickerNodeId = hit.nodeId();
        colorPickerStyleName = hit.data().getOrDefault("style-name", "");
        colorPickerStyleValue = hit.data().getOrDefault("style-value", "");
        colorPickerX = hit.rect().x + 4f;
        colorPickerY = hit.rect().y + 34f;
    }

    private void openValueDropdown(ActionHit hit) {
        valueDropdownOpen = true;
        colorPickerOpen = false;
        valueDropdownNodeId = hit.nodeId();
        valueDropdownStyleName = hit.data().getOrDefault("style-name", "");
        valueDropdownStyleValue = hit.data().getOrDefault("style-value", "");
        valueDropdownX = hit.rect().x;
        valueDropdownY = hit.rect().y + 34f;
    }

    private void closeColorPicker() {
        colorPickerOpen = false;
        colorPickerNodeId = 0;
        colorPickerStyleName = "";
        colorPickerStyleValue = "";
    }

    private void closeValueDropdown() {
        valueDropdownOpen = false;
        valueDropdownNodeId = 0;
        valueDropdownStyleName = "";
        valueDropdownStyleValue = "";
    }

    private void drawColorSwatch(UiRenderContext ctx, UiRect rect, String value) {
        UiColor color = colorValue(value);
        if (isTransparentColor(value)) {
            ctx.fill(rect, UiColor.rgba255(255, 255, 255, 255));
            ctx.fill(new UiRect(rect.x, rect.y, rect.w * 0.5f, rect.h * 0.5f), UiColor.rgba255(226, 232, 240, 255));
            ctx.fill(new UiRect(rect.x + rect.w * 0.5f, rect.y + rect.h * 0.5f, rect.w * 0.5f, rect.h * 0.5f), UiColor.rgba255(226, 232, 240, 255));
        } else {
            ctx.fill(rect, color);
        }
        ctx.stroke(rect, BORDER, 1f);
        ctx.stroke(new UiRect(rect.x + 1f, rect.y + 1f, Math.max(0f, rect.w - 2f), Math.max(0f, rect.h - 2f)), UiColor.rgba255(255, 255, 255, 160), 1f);
    }

    private boolean isColorProperty(String name) {
        String n = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        return n.equals("color") || n.endsWith("-color") || n.equals("caret-color") || n.equals("outline-color");
    }

    private boolean isTransparentColor(String value) {
        return value == null || value.trim().isBlank() || "transparent".equalsIgnoreCase(value.trim());
    }

    private UiColor colorValue(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (v.startsWith("#") && (v.length() == 7 || v.length() == 4)) {
            try {
                if (v.length() == 4) {
                    int r = Integer.parseInt(v.substring(1, 2) + v.substring(1, 2), 16);
                    int g = Integer.parseInt(v.substring(2, 3) + v.substring(2, 3), 16);
                    int b = Integer.parseInt(v.substring(3, 4) + v.substring(3, 4), 16);
                    return UiColor.rgba255(r, g, b, 255);
                }
                int r = Integer.parseInt(v.substring(1, 3), 16);
                int g = Integer.parseInt(v.substring(3, 5), 16);
                int b = Integer.parseInt(v.substring(5, 7), 16);
                return UiColor.rgba255(r, g, b, 255);
            } catch (RuntimeException ignored) {
                return UiColor.rgba255(148, 163, 184, 255);
            }
        }
        if ("white".equals(v)) return UiColor.rgba255(255, 255, 255, 255);
        if ("black".equals(v)) return UiColor.rgba255(0, 0, 0, 255);
        if ("red".equals(v)) return UiColor.rgba255(239, 68, 68, 255);
        if ("green".equals(v)) return UiColor.rgba255(34, 197, 94, 255);
        if ("blue".equals(v)) return UiColor.rgba255(59, 130, 246, 255);
        return UiColor.rgba255(148, 163, 184, 255);
    }

    private List<String> dropdownOptions(String property) {
        String p = property == null ? "" : property.trim().toLowerCase(Locale.ROOT);
        return switch (p) {
            case "display" -> List.of("block", "inline", "inline-block", "flex", "grid", "none");
            case "position" -> List.of("static", "relative", "absolute", "fixed", "sticky");
            case "overflow", "overflow-x", "overflow-y" -> List.of("visible", "hidden", "auto", "scroll");
            case "cursor" -> List.of("auto", "default", "pointer", "text", "move", "grab", "crosshair", "not-allowed");
            case "border-style", "outline-style" -> List.of("none", "solid", "dashed", "dotted", "double");
            case "box-sizing" -> List.of("content-box", "border-box");
            case "text-align" -> List.of("left", "center", "right", "justify");
            case "white-space" -> List.of("normal", "nowrap", "pre", "pre-wrap", "pre-line");
            case "flex-direction" -> List.of("row", "row-reverse", "column", "column-reverse");
            case "align-items" -> List.of("stretch", "flex-start", "center", "flex-end", "baseline");
            case "justify-content" -> List.of("flex-start", "center", "flex-end", "space-between", "space-around", "space-evenly");
            case "font-weight" -> List.of("normal", "bold", "100", "200", "300", "400", "500", "600", "700", "800", "900");
            default -> List.of();
        };
    }

    private void contextMenu(UiRenderContext ctx) {
        if (!contextMenuOpen || contextMenuNodeId <= 0) return;
        float w = 168f;
        float h = 92f;
        float x = clamp(contextMenuX, windowRect.x + 8f, windowRect.x + windowRect.w - w - 8f);
        float y = clamp(contextMenuY - h, windowRect.y + 8f, windowRect.y + windowRect.h - h - 8f);
        UiRect rect = new UiRect(x, y, w, h);
        ctx.fill(rect, PANEL);
        ctx.stroke(rect, ACCENT, 1f);
        contextMenuItem(ctx, editIcon, "Edit element", "devtools.node.context.edit", x + 6f, y + 62f, w - 12f, contextMenuNodeId);
        contextMenuItem(ctx, copyIcon, "Duplicate", "devtools.node.context.duplicate", x + 6f, y + 34f, w - 12f, contextMenuNodeId);
        contextMenuItem(ctx, trashIcon, "Delete", "devtools.node.context.delete", x + 6f, y + 6f, w - 12f, contextMenuNodeId);
    }

    private void contextMenuItem(UiRenderContext ctx, UiIcon icon, String label, String action, float x, float y, float w, int nodeId) {
        UiRect rect = new UiRect(x, y, w, 24f);
        ctx.fill(rect, CARD);
        ctx.stroke(rect, BORDER, 1f);
        if (icon != null && ctx.supportsIcons()) ctx.icon(icon, new UiRect(x + 8f, y + 6f, 12f, 12f), 0.58f, ACCENT, TextAlign.CENTER);
        text(ctx, label, x + 28f, y + 7f, w - 36f, 11f, 0.50f, TEXT, FONT_BOLD);
        actionHits.add(new ActionHit(action, Map.of(), nodeId, rect));
    }

    private List<HtmlElementSnapshot> visibleElements(HtmlDevToolsSnapshot snapshot) {
        Map<Integer, List<HtmlElementSnapshot>> byParent = new HashMap<>();
        for (HtmlElementSnapshot element : snapshot.elements()) {
            byParent.computeIfAbsent(element.parent(), ignored -> new ArrayList<>()).add(element);
        }
        ArrayList<HtmlElementSnapshot> out = new ArrayList<>();
        appendVisible(0, byParent, out);
        return out;
    }

    private void appendVisible(int parent, Map<Integer, List<HtmlElementSnapshot>> byParent, List<HtmlElementSnapshot> out) {
        List<HtmlElementSnapshot> children = byParent.get(parent);
        if (children == null) return;
        for (HtmlElementSnapshot child : children) {
            out.add(child);
            if (!collapsedNodeKeys.contains(collapseKey(child))) appendVisible(child.id(), byParent, out);
        }
    }

    private String collapseKey(HtmlElementSnapshot element) {
        if (element == null) return "";
        if (!element.path().isBlank()) return element.path();
        if (!element.selector().isBlank()) return element.selector();
        return element.tag() + "#" + element.id();
    }

    private boolean forwardRemote(ActionHit hit) {
        if (hit == null) return false;
        String action = hit.actionId();
        if ("devtools.node.toggle".equals(action)
                || "devtools.style.edit.name".equals(action)
                || "devtools.style.edit.value".equals(action)
                || "devtools.style.add".equals(action)
                || "devtools.color.open".equals(action)
                || "devtools.value.dropdown".equals(action)) {
            return false;
        }
        return HtmlDevToolsRuntime.dispatchRemoteAction(action, hit.nodeId(), hit.data());
    }

    private void dispatch(ActionHit hit) {
        if (hit == null) return;
        if (forwardRemote(hit)) {
            closeContextMenu();
            closeColorPicker();
            closeValueDropdown();
            return;
        }
        if ("devtools.undo".equals(hit.actionId())) {
            if (HtmlDevToolsRuntime.undo()) statusMessage = "undo";
            return;
        }
        if ("devtools.redo".equals(hit.actionId())) {
            if (HtmlDevToolsRuntime.redo()) statusMessage = "redo";
            return;
        }
        if ("devtools.save".equals(hit.actionId())) {
            HtmlDevToolsRuntime.saveChanges();
            statusMessage = "saved";
            return;
        }
        if ("devtools.tab.select".equals(hit.actionId())) {
            controller.selectTab(hit.data().get("tab"));
            closeContextMenu();
            return;
        }
        if ("devtools.node.select".equals(hit.actionId())) {
            controller.selectNode(hit.nodeId());
            closeContextMenu();
            return;
        }
        if ("devtools.node.toggle".equals(hit.actionId())) {
            String key = hit.data().getOrDefault("collapse-key", String.valueOf(hit.nodeId()));
            if (!collapsedNodeKeys.add(key)) collapsedNodeKeys.remove(key);
            closeContextMenu();
            return;
        }
        if ("devtools.style.toggle".equals(hit.actionId())) {
            HtmlDevToolsRuntime.toggleStyle(hit.nodeId(), hit.data().get("style-name"), hit.data().get("style-value"));
            closeColorPicker();
            closeValueDropdown();
            return;
        }
        if ("devtools.color.open".equals(hit.actionId())) {
            openColorPicker(hit);
            closeContextMenu();
            return;
        }
        if ("devtools.value.dropdown".equals(hit.actionId())) {
            openValueDropdown(hit);
            closeContextMenu();
            return;
        }
        if ("devtools.color.pick".equals(hit.actionId()) || "devtools.value.option".equals(hit.actionId())) {
            HtmlDevToolsRuntime.applyStyle(hit.nodeId(), hit.data().get("style-name"), hit.data().get("style-name"), hit.data().get("style-value"));
            statusMessage = "modified";
            closeColorPicker();
            closeValueDropdown();
            return;
        }
        if ("devtools.style.edit.name".equals(hit.actionId()) || "devtools.style.edit.value".equals(hit.actionId())) {
            beginStyleEdit(hit.nodeId(), hit.data().get("style-name"), hit.data().get("style-value"), hit.actionId().endsWith("value"), false);
            return;
        }
        if ("devtools.style.add".equals(hit.actionId())) {
            beginStyleEdit(hit.nodeId(), "", "", false, true);
            return;
        }
        if ("devtools.node.context.delete".equals(hit.actionId())) {
            if (HtmlDevToolsRuntime.deleteNode(hit.nodeId())) {
                controller.selectNode(0);
                statusMessage = "deleted";
            } else {
                statusMessage = "protected node";
            }
            closeContextMenu();
            return;
        }
        if ("devtools.node.context.duplicate".equals(hit.actionId())) {
            int duplicate = HtmlDevToolsRuntime.duplicateNode(hit.nodeId());
            if (duplicate > 0) controller.selectNode(duplicate);
            closeContextMenu();
            return;
        }
        if ("devtools.node.context.edit".equals(hit.actionId())) {
            HtmlDevToolsRuntime.markNodeEditing(hit.nodeId());
            controller.selectNode(hit.nodeId());
            controller.selectTab(HtmlDevToolsTab.STYLES.id());
            closeContextMenu();
            return;
        }
        if ("devtools.pick.toggle".equals(hit.actionId())) {
            controller.togglePicker();
            closeContextMenu();
            return;
        }
        if ("devtools.close".equals(hit.actionId())) {
            if (closeRequested != null) closeRequested.run();
            else setVisible(false);
        }
    }

    private void beginStyleEdit(int nodeId, String name, String value, boolean valueField, boolean add) {
        editingStyleNodeId = Math.max(0, nodeId);
        editingStyleName = name == null ? "" : name.trim();
        editingStyleDraftName = editingStyleName;
        editingStyleDraftValue = value == null ? "" : value.trim();
        editingStyleValue = valueField;
        addingStyle = add;
    }

    private boolean editingStyle() {
        return editingStyleNodeId > 0;
    }

    private void cancelStyleEdit() {
        editingStyleNodeId = 0;
        editingStyleName = "";
        editingStyleDraftName = "";
        editingStyleDraftValue = "";
        editingStyleValue = false;
        addingStyle = false;
    }

    private void commitStyleEdit() {
        if (editingStyleDraftName.isBlank()) {
            cancelStyleEdit();
            return;
        }
        Map<String, String> data = Map.of(
                "old-name", editingStyleName,
                "style-name", editingStyleDraftName,
                "style-value", editingStyleDraftValue
        );
        boolean remote = HtmlDevToolsRuntime.dispatchRemoteAction(addingStyle ? "devtools.style.add.commit" : "devtools.style.edit.commit", editingStyleNodeId, data);
        if (!remote) {
            if (addingStyle) HtmlDevToolsRuntime.addStyle(editingStyleNodeId, editingStyleDraftName, editingStyleDraftValue);
            else HtmlDevToolsRuntime.applyStyle(editingStyleNodeId, editingStyleName, editingStyleDraftName, editingStyleDraftValue);
        }
        statusMessage = "modified";
        cancelStyleEdit();
    }

    private void appendEditText(String value) {
        if (value == null || value.isEmpty()) return;
        if (editingStyleValue) editingStyleDraftValue += value;
        else editingStyleDraftName += value;
    }

    private void backspaceEditText() {
        if (editingStyleValue) editingStyleDraftValue = removeLast(editingStyleDraftValue);
        else editingStyleDraftName = removeLast(editingStyleDraftName);
    }

    private String removeLast(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, value.length() - 1);
    }

    private String styleKey(int nodeId, String name) {
        return Math.max(0, nodeId) + "|" + (name == null ? "" : name.trim().toLowerCase(Locale.ROOT));
    }

    private ActionHit actionAt(float x, float y) {
        for (int i = actionHits.size() - 1; i >= 0; i--) {
            ActionHit hit = actionHits.get(i);
            if (contains(hit.rect(), x, y)) return hit;
        }
        return null;
    }

    private HtmlElementSnapshot elementById(HtmlDevToolsSnapshot snapshot, int id) {
        if (snapshot == null || id <= 0) return null;
        for (HtmlElementSnapshot element : snapshot.elements()) if (element.id() == id) return element;
        return null;
    }

    private int rows(float h) {
        return Math.max(1, (int) ((h - 34f) / 18f));
    }

    private void badge(UiRenderContext ctx, String value, float x, float y, UiColor color) {
        UiRect rect = new UiRect(x, y, 62f, 18f);
        ctx.fill(rect, UiColor.rgba255(240, 249, 255, 255));
        ctx.stroke(rect, color, 1f);
        text(ctx, value, rect.x, rect.y + 4f, rect.w, 11f, 0.52f, color, FONT_BOLD, TextAlign.CENTER);
    }

    private UiColor displayColor(String display) {
        return "flex".equals(display) ? GOOD : ACCENT;
    }

    private static boolean contains(UiRect rect, float x, float y) {
        return rect != null && x >= rect.x && x <= rect.x + rect.w && y >= rect.y && y <= rect.y + rect.h;
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        if (max <= 1) return "…";
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static float charWidth(String value, float scale) {
        return (value == null ? 0 : value.length()) * 10.8f * Math.max(0.1f, scale);
    }

    private void text(UiRenderContext ctx, String value, float x, float y, float w, float h, float scale, UiColor color, String fontId) {
        text(ctx, value, x, y, w, h, scale, color, fontId, TextAlign.LEFT);
    }

    private void text(UiRenderContext ctx, String value, float x, float y, float w, float h, float scale, UiColor color, String fontId, TextAlign align) {
        ctx.text(value == null ? "" : value, new UiRect(x, y, w, h), scale, color, align, fontId);
    }

    private record ActionHit(String actionId, Map<String, String> data, int nodeId, UiRect rect) {
        ActionHit {
            actionId = actionId == null ? "" : actionId.trim();
            data = data == null ? Map.of() : Map.copyOf(data);
            nodeId = Math.max(0, nodeId);
        }
    }
}
