package dev.takesome.helix.devTools;

public final class HtmlDevToolsController {
    private final HtmlDevToolsSnapshotFactory snapshots = new HtmlDevToolsSnapshotFactory();
    private HtmlDevToolsSession session = HtmlDevToolsSession.closed();
    private HtmlInspectionTarget target = HtmlInspectionTarget.empty();

    public HtmlDevToolsSession session() { return session; }
    public HtmlInspectionTarget target() { return target; }

    public HtmlDevToolsSession toggle() { session = session.toggle(); return session; }
    public HtmlDevToolsSession open() { session = session.opened(); return session; }
    public HtmlDevToolsSession close() { session = session.closedSession(); return session; }
    public HtmlDevToolsSession selectTab(String tabId) { session = session.selectTab(HtmlDevToolsTab.fromId(tabId)); return session; }
    public HtmlDevToolsSession selectNode(int nodeId) { session = session.selectNode(nodeId); return session; }
    public HtmlDevToolsSession togglePicker() { session = session.togglePicker(); return session; }

    public HtmlDevToolsSnapshot inspect(HtmlInspectionTarget nextTarget) {
        target = nextTarget == null ? HtmlInspectionTarget.empty() : nextTarget;
        return snapshot();
    }

    public HtmlDevToolsSnapshot snapshot() {
        return snapshots.create(session, target);
    }
}
