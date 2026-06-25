package dev.takesome.helix.devTools;

import dev.takesome.helix.ui.runtime.UiRuntimeInspectionSource;

public final class HtmlDevToolsPanel {
    private final HtmlDevToolsController controller;

    public HtmlDevToolsPanel() { this(new HtmlDevToolsController()); }
    public HtmlDevToolsPanel(HtmlDevToolsController controller) { this.controller = controller == null ? new HtmlDevToolsController() : controller; }
    public HtmlDevToolsController controller() { return controller; }

    public HtmlDevToolsSnapshot inspect(UiRuntimeInspectionSource source) {
        return controller.inspect(HtmlInspectionTarget.runtime(source));
    }

    public HtmlDevToolsSnapshot inspect(HtmlInspectionTarget target) { return controller.inspect(target); }
    public HtmlDevToolsSnapshot open(HtmlInspectionTarget target) { controller.open(); return controller.inspect(target); }
    public HtmlDevToolsSnapshot close() { controller.close(); return controller.snapshot(); }
}
