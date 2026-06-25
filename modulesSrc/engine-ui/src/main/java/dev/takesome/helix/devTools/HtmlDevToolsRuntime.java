package dev.takesome.helix.devTools;

import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.dom.UiDomNode;
import dev.takesome.helix.ui.dom.UiDomText;
import dev.takesome.helix.ui.runtime.UiRuntimeInspectionSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class HtmlDevToolsRuntime {
    private static volatile UiRuntimeInspectionSource source = UiRuntimeInspectionSource.empty();
    private static volatile int highlightedNodeId;
    private static volatile boolean dirty;
    private static volatile long lastSavedMillis;
    private static volatile boolean pageRebuildRequested;
    private static volatile UiRuntimeInspectionSource pendingRebuildSource;
    private static volatile HtmlDevToolsSnapshot remoteSnapshot;
    private static volatile Consumer<HtmlDevToolsRemoteAction> remoteActionSink;

    private static final int MAX_HISTORY = 64;
    private static final Map<String, String> originalStyleValues = new HashMap<>();
    private static final Set<String> disabledStyles = new HashSet<>();
    private static final Deque<DomHistoryEntry> undoStack = new ArrayDeque<>();
    private static final Deque<DomHistoryEntry> redoStack = new ArrayDeque<>();

    private HtmlDevToolsRuntime() { }


    public static void setRemoteSnapshot(HtmlDevToolsSnapshot snapshot) {
        remoteSnapshot = snapshot;
    }

    public static HtmlDevToolsSnapshot remoteSnapshot() {
        return remoteSnapshot;
    }

    public static void clearRemoteSnapshot() {
        remoteSnapshot = null;
    }

    public static void setRemoteActionSink(Consumer<HtmlDevToolsRemoteAction> sink) {
        remoteActionSink = sink;
    }

    public static boolean dispatchRemoteAction(String actionId, int nodeId, Map<String, String> data) {
        Consumer<HtmlDevToolsRemoteAction> sink = remoteActionSink;
        if (sink == null) return false;
        sink.accept(new HtmlDevToolsRemoteAction(actionId, nodeId, data));
        return true;
    }

    public static void update(UiRuntimeInspectionSource nextSource) {
        source = nextSource == null ? UiRuntimeInspectionSource.empty() : nextSource;
    }

    public static UiRuntimeInspectionSource source() {
        return source == null ? UiRuntimeInspectionSource.empty() : source;
    }

    public static HtmlInspectionTarget target() {
        return HtmlInspectionTarget.runtime(source());
    }

    public static void highlightNode(int nodeId) {
        highlightedNodeId = Math.max(0, nodeId);
    }

    public static int highlightedNodeId() {
        return Math.max(0, highlightedNodeId);
    }

    public static void clearHighlight() {
        highlightedNodeId = 0;
    }

    public static synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public static synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public static synchronized boolean undo() {
        DomHistoryEntry previous = undoStack.pollLast();
        if (previous == null) return false;
        DomHistoryEntry current = captureHistoryEntry();
        if (current != null) redoStack.addLast(current);
        restoreHistoryEntry(previous);
        return true;
    }

    public static synchronized boolean redo() {
        DomHistoryEntry next = redoStack.pollLast();
        if (next == null) return false;
        DomHistoryEntry current = captureHistoryEntry();
        if (current != null) undoStack.addLast(current);
        restoreHistoryEntry(next);
        return true;
    }

    public static synchronized boolean styleDisabled(int nodeId, String property) {
        return disabledStyles.contains(styleKey(nodeId, property));
    }

    public static synchronized String styleSnapshotValue(int nodeId, String property, String currentValue) {
        String key = styleKey(nodeId, property);
        if (!disabledStyles.contains(key)) return currentValue == null ? "" : currentValue;
        return originalStyleValues.getOrDefault(key, currentValue == null ? "" : currentValue);
    }

    public static synchronized void toggleStyle(int nodeId, String property, String currentValue) {
        String name = cleanProperty(property);
        if (nodeId <= 0 || name.isBlank()) return;
        UiDomElement element = findElement(nodeId);
        if (element == null) return;
        pushUndoSnapshot();

        String key = styleKey(nodeId, name);
        originalStyleValues.putIfAbsent(key, valueOr(currentValue, element.style(name, "")));
        if (disabledStyles.remove(key)) {
            element.setComputedStyle(name, originalStyleValues.getOrDefault(key, currentValue));
        } else {
            disabledStyles.add(key);
            element.setComputedStyle(name, "");
        }
        dirty = true;
        requestPageRebuild();
    }

    public static synchronized void applyStyle(int nodeId, String oldProperty, String nextProperty, String nextValue) {
        String oldName = cleanProperty(oldProperty);
        String newName = cleanProperty(nextProperty);
        if (nodeId <= 0 || newName.isBlank()) return;
        UiDomElement element = findElement(nodeId);
        if (element == null) return;
        pushUndoSnapshot();

        if (!oldName.isBlank() && !oldName.equals(newName)) {
            String oldKey = styleKey(nodeId, oldName);
            originalStyleValues.putIfAbsent(oldKey, element.style(oldName, ""));
            disabledStyles.remove(oldKey);
            element.setComputedStyle(oldName, "");
        }
        String newKey = styleKey(nodeId, newName);
        originalStyleValues.putIfAbsent(newKey, element.style(newName, ""));
        disabledStyles.remove(newKey);
        element.setComputedStyle(newName, valueOr(nextValue, ""));
        dirty = true;
        requestPageRebuild();
    }

    public static synchronized void addStyle(int nodeId, String property, String value) {
        applyStyle(nodeId, "", property, value);
    }

    public static synchronized boolean deleteNode(int nodeId) {
        UiDomElement element = findElement(nodeId);
        if (element == null || !element.hasParent() || protectedElement(element)) return false;
        pushUndoSnapshot();
        element.parent().removeChild(element);
        clearHighlight();
        dirty = true;
        requestPageRebuild();
        return true;
    }

    public static synchronized int duplicateNode(int nodeId) {
        UiDomElement element = findElement(nodeId);
        if (element == null || !element.hasParent() || protectedElement(element)) return 0;
        pushUndoSnapshot();
        UiDomElement clone = cloneElement(element);
        UiDomElement parent = element.parent();
        int index = parent.children().indexOf(element);
        parent.insertChild(index + 1, clone);
        dirty = true;
        requestPageRebuild();
        return clone.nodeId();
    }

    public static synchronized boolean markNodeEditing(int nodeId) {
        return findElement(nodeId) != null;
    }

    public static synchronized boolean saveChanges() {
        dirty = false;
        lastSavedMillis = System.currentTimeMillis();
        return true;
    }


    public static boolean consumePageRebuildRequest() {
        boolean requested = pageRebuildRequested;
        pageRebuildRequested = false;
        return requested;
    }

    private static void requestPageRebuild() {
        // Live recompilation from the inspection DOM is not safe yet: it can rebuild
        // an incomplete document and leave the rendered page black. Keep DevTools
        // mutations sandboxed in the inspection DOM until the renderer has a safe
        // source-preserving patch/apply pipeline.
        pageRebuildRequested = false;
        pendingRebuildSource = null;
    }

    public static boolean hasUnsavedChanges() {
        return dirty;
    }

    public static long lastSavedMillis() {
        return lastSavedMillis;
    }


    private static void pushUndoSnapshot() {
        DomHistoryEntry entry = captureHistoryEntry();
        if (entry == null) return;
        undoStack.addLast(entry);
        while (undoStack.size() > MAX_HISTORY) undoStack.removeFirst();
        redoStack.clear();
    }

    private static DomHistoryEntry captureHistoryEntry() {
        UiRuntimeInspectionSource current = source();
        if (current == null || current.document() == null || current.document().rootOptional().isEmpty()) return null;
        return new DomHistoryEntry(
                cloneDocument(current.document()),
                Map.copyOf(originalStyleValues),
                Set.copyOf(disabledStyles)
        );
    }

    private static void restoreHistoryEntry(DomHistoryEntry entry) {
        UiRuntimeInspectionSource current = source();
        if (entry == null || current == null || current.document() == null || entry.document().rootOptional().isEmpty()) return;
        current.document().setRoot(cloneElementInto(current.document(), entry.document().documentElement()));
        current.document().drainMutations();
        current.document().root().clearDirty();
        originalStyleValues.clear();
        originalStyleValues.putAll(entry.originalStyleValues());
        disabledStyles.clear();
        disabledStyles.addAll(entry.disabledStyles());
        clearHighlight();
        dirty = true;
        requestPageRebuild();
    }

    private static UiDomDocument cloneDocument(UiDomDocument sourceDocument) {
        UiDomDocument out = new UiDomDocument();
        out.setRoot(cloneElementInto(out, sourceDocument.documentElement()));
        out.drainMutations();
        out.root().clearDirty();
        return out;
    }

    private static UiDomElement cloneElementInto(UiDomDocument target, UiDomElement sourceElement) {
        UiDomElement out = target.createElement(sourceElement.tagName());
        for (Map.Entry<String, String> entry : sourceElement.attributes().entrySet()) out.setAttribute(entry.getKey(), entry.getValue());
        for (Map.Entry<String, String> entry : sourceElement.computedStyle().entrySet()) out.setComputedStyle(entry.getKey(), entry.getValue());
        for (UiDomNode child : new ArrayList<>(sourceElement.children())) out.appendChild(cloneNodeInto(target, child));
        return out;
    }

    private static UiDomNode cloneNodeInto(UiDomDocument target, UiDomNode node) {
        if (node instanceof UiDomElement element) return cloneElementInto(target, element);
        if (node instanceof UiDomText text) return target.createText(text.textContent());
        return target.createText(node.textContent());
    }

    public static UiDomElement findElement(int nodeId) {
        UiRuntimeInspectionSource current = source();
        if (nodeId <= 0 || current.document() == null) return null;
        return current.document().rootOptional().map(root -> findElement(root, nodeId)).orElse(null);
    }

    private static UiDomElement findElement(UiDomElement current, int nodeId) {
        if (current.nodeId() == nodeId) return current;
        for (UiDomNode child : current.children()) {
            if (child instanceof UiDomElement element) {
                UiDomElement found = findElement(element, nodeId);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static UiDomElement cloneElement(UiDomElement sourceElement) {
        UiDomElement out = sourceElement.ownerDocument().createElement(sourceElement.tagName());
        for (Map.Entry<String, String> entry : sourceElement.attributes().entrySet()) out.setAttribute(entry.getKey(), entry.getValue());
        for (Map.Entry<String, String> entry : sourceElement.computedStyle().entrySet()) out.setComputedStyle(entry.getKey(), entry.getValue());
        for (UiDomNode child : new ArrayList<>(sourceElement.children())) out.appendChild(cloneNode(child));
        return out;
    }

    private static UiDomNode cloneNode(UiDomNode node) {
        if (node instanceof UiDomElement element) return cloneElement(element);
        if (node instanceof UiDomText text) return text.ownerDocument().createText(text.textContent());
        return node.ownerDocument().createText(node.textContent());
    }

    private static String styleKey(int nodeId, String property) {
        return Math.max(0, nodeId) + "|" + cleanProperty(property);
    }

    private static boolean protectedElement(UiDomElement element) {
        if (element == null) return true;
        String tag = element.tagName();
        return "html".equals(tag) || "head".equals(tag) || "body".equals(tag);
    }

    private static String cleanProperty(String property) {
        return property == null ? "" : property.trim().toLowerCase(Locale.ROOT);
    }

    private static String valueOr(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isBlank() ? (fallback == null ? "" : fallback.trim()) : clean;
    }

    private record DomHistoryEntry(
            UiDomDocument document,
            Map<String, String> originalStyleValues,
            Set<String> disabledStyles
    ) { }
}
