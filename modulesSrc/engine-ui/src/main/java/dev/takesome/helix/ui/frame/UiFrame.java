package dev.takesome.helix.ui.frame;

import java.util.List;

/** Fully resolved UI runtime frame. Authoring formats must not cross this boundary. */
public final class UiFrame {
    private final float width;
    private final float height;
    private final List<UiFrameNode> nodes;
    private final List<UiFrameCommand> commands;

    public UiFrame(float width, float height, List<UiFrameNode> nodes, List<UiFrameCommand> commands) {
        this.width = Float.isFinite(width) ? Math.max(0f, width) : 0f;
        this.height = Float.isFinite(height) ? Math.max(0f, height) : 0f;
        this.nodes = List.copyOf(nodes == null ? List.of() : nodes);
        this.commands = List.copyOf(commands == null ? List.of() : commands);
    }

    public static UiFrame empty() {
        return new UiFrame(0f, 0f, List.of(), List.of());
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public List<UiFrameNode> nodes() {
        return nodes;
    }

    public List<UiFrameCommand> commands() {
        return commands;
    }
}
