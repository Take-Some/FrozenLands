package dev.takesome.helix.ui.frame;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiBoxShadow;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.render.UiRenderContext;

import java.util.ArrayList;
import java.util.List;

/** UiRenderContext implementation that records retained-node output into UiFrame commands. */
public final class UiFrameRecorder implements UiRenderContext {
    private final UiRenderContext metrics;
    private final UiFrameRenderCapabilities capabilities;
    private final ArrayList<UiFrameCommand> commands = new ArrayList<>();

    public UiFrameRecorder(UiRenderContext metrics, UiFrameRenderCapabilities capabilities) {
        this.metrics = metrics;
        this.capabilities = capabilities == null ? UiFrameRenderCapabilities.conservative() : capabilities;
    }

    public List<UiFrameCommand> commands() {
        return List.copyOf(commands);
    }

    @Override
    public void fill(UiRect rect, UiColor color) {
        commands.add(UiFrameCommand.fill(rect, color));
    }

    @Override
    public void stroke(UiRect rect, UiColor color, float width) {
        commands.add(UiFrameCommand.stroke(rect, color, width));
    }

    @Override
    public void boxShadow(UiRect rect, UiBoxShadow shadow) {
        commands.add(UiFrameCommand.boxShadow(rect, shadow));
    }

    @Override
    public void text(String text, UiRect rect, float scale, UiColor color, TextAlign align) {
        commands.add(UiFrameCommand.text(text, rect, scale, color, align, ""));
    }

    @Override
    public void text(String text, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) {
        commands.add(UiFrameCommand.text(text, rect, scale, color, align, fontId));
    }

    @Override
    public void buttonText(String text, UiRect rect, float scale, UiColor color, TextAlign align) {
        commands.add(UiFrameCommand.buttonText(text, rect, scale, color, align));
    }

    @Override
    public void buttonText(String text, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) {
        commands.add(UiFrameCommand.buttonText(text, rect, scale, color, align, fontId));
    }

    @Override
    public boolean supportsIcons() {
        return capabilities.icons();
    }

    @Override
    public boolean supportsElements() {
        return capabilities.elements();
    }

    @Override
    public boolean supportsImages() {
        return capabilities.images();
    }

    @Override
    public boolean icon(UiIcon icon, UiRect rect, float scale, UiColor color, TextAlign align) {
        if (!capabilities.icons()) return false;
        commands.add(UiFrameCommand.icon(icon, rect, scale, color, align));
        return true;
    }

    @Override
    public boolean image(String source, UiRect rect, float opacity) {
        if (!capabilities.images()) return false;
        commands.add(UiFrameCommand.image(source, rect, opacity));
        return true;
    }

    @Override
    public boolean drawElement(UiElementSkin element, UiRect rect, UiColor tint) {
        if (!capabilities.elements()) return false;
        commands.add(UiFrameCommand.element(element, rect, tint));
        return true;
    }

    @Override
    public UiRect elementContentBounds(UiElementSkin element, UiRect rect) {
        return metrics == null ? rect : metrics.elementContentBounds(element, rect);
    }

    @Override
    public boolean drawTracers() {
        return metrics != null && metrics.drawTracers();
    }

    @Override
    public void traceNode(UiRect bounds, UiRect contentBounds, int depth, String label) {
        commands.add(UiFrameCommand.traceNode(bounds, contentBounds, depth, label));
    }
}
