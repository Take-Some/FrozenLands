package dev.takesome.helix.ui.frame;

import dev.takesome.helix.ui.model.UiRect;

import java.util.List;

/** Resolved UI node in the runtime frame IR. */
public final class UiFrameNode {
    public static final int NO_PARENT = -1;

    private final int id;
    private final int parentId;
    private final UiFrameNodeKind kind;
    private final UiRect bounds;
    private final boolean visible;
    private final boolean enabled;
    private final int depth;
    private final UiFrameStyle style;
    private final List<UiFrameBindingValue> bindings;
    private final List<UiFrameAction> actions;

    public UiFrameNode(
            int id,
            int parentId,
            UiFrameNodeKind kind,
            UiRect bounds,
            boolean visible,
            boolean enabled,
            int depth,
            UiFrameStyle style,
            List<UiFrameBindingValue> bindings,
            List<UiFrameAction> actions
    ) {
        this.id = id;
        this.parentId = parentId;
        this.kind = kind == null ? UiFrameNodeKind.CUSTOM : kind;
        this.bounds = bounds == null ? new UiRect(0f, 0f, 0f, 0f) : bounds;
        this.visible = visible;
        this.enabled = enabled;
        this.depth = Math.max(0, depth);
        this.style = style == null ? UiFrameStyle.empty() : style;
        this.bindings = List.copyOf(bindings == null ? List.of() : bindings);
        this.actions = List.copyOf(actions == null ? List.of() : actions);
    }

    public int id() {
        return id;
    }

    public int parentId() {
        return parentId;
    }

    public UiFrameNodeKind kind() {
        return kind;
    }

    public UiRect bounds() {
        return bounds;
    }

    public boolean visible() {
        return visible;
    }

    public boolean enabled() {
        return enabled;
    }

    public int depth() {
        return depth;
    }

    public UiFrameStyle style() {
        return style;
    }

    public List<UiFrameBindingValue> bindings() {
        return bindings;
    }

    public List<UiFrameAction> actions() {
        return actions;
    }
}
