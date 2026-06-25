package dev.takesome.helix.devTools;

import java.io.Serializable;

public record HtmlDevToolsSession(boolean open, HtmlDevToolsTab selectedTab, int selectedNodeId, boolean pickerEnabled) implements Serializable {
    public HtmlDevToolsSession {
        selectedTab = selectedTab == null ? HtmlDevToolsTab.ELEMENTS : selectedTab;
        selectedNodeId = Math.max(0, selectedNodeId);
    }

    public static HtmlDevToolsSession closed() {
        return new HtmlDevToolsSession(false, HtmlDevToolsTab.ELEMENTS, 0, false);
    }

    public HtmlDevToolsSession opened() {
        return new HtmlDevToolsSession(true, selectedTab, selectedNodeId, pickerEnabled);
    }

    public HtmlDevToolsSession closedSession() {
        return new HtmlDevToolsSession(false, selectedTab, selectedNodeId, false);
    }

    public HtmlDevToolsSession toggle() {
        return open ? closedSession() : opened();
    }

    public HtmlDevToolsSession selectTab(HtmlDevToolsTab tab) {
        return new HtmlDevToolsSession(open, tab == null ? selectedTab : tab, selectedNodeId, pickerEnabled);
    }

    public HtmlDevToolsSession selectNode(int nodeId) {
        return new HtmlDevToolsSession(open, selectedTab, Math.max(0, nodeId), pickerEnabled);
    }

    public HtmlDevToolsSession togglePicker() {
        return new HtmlDevToolsSession(open, selectedTab, selectedNodeId, !pickerEnabled);
    }
}
