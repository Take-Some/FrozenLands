package dev.takesome.helix.ui.legacy.render;

import java.util.Comparator;
import dev.takesome.helix.ui.definition.UiPanelDefinition;
import dev.takesome.helix.ui.model.UiRect;

public final class UiPanelRenderEntry {
    public static final Comparator<UiPanelRenderEntry> ORDER = Comparator
            .comparingInt((UiPanelRenderEntry entry) -> entry.panel.layer)
            .thenComparingInt(entry -> entry.index);

    public final int index;
    public final UiPanelDefinition panel;
    public final UiRect rect;

    public UiPanelRenderEntry(int index, UiPanelDefinition panel, UiRect rect) {
        this.index = index;
        this.panel = panel;
        this.rect = rect;
    }
}
