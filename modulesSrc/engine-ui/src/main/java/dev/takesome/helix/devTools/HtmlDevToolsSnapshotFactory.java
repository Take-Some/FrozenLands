package dev.takesome.helix.devTools;

import dev.takesome.helix.devTools.css.HtmlCssBoxSnapshot;
import dev.takesome.helix.devTools.css.HtmlCssPropertyCatalog;
import dev.takesome.helix.devTools.css.HtmlCssPropertySnapshot;
import dev.takesome.helix.devTools.dom.HtmlElementSnapshot;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.runtime.UiRuntimeInspectionSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/** Builds Chromium-like DevTools snapshots from the live HELIX UI runtime inspection source. */
public final class HtmlDevToolsSnapshotFactory {
    private final HtmlCssPropertyCatalog css = HtmlCssPropertyCatalog.builtins();

    public HtmlDevToolsSnapshot create(HtmlDevToolsSession session, HtmlInspectionTarget target) {
        HtmlDevToolsSession safeSession = session == null ? HtmlDevToolsSession.closed() : session;
        HtmlInspectionTarget safeTarget = target == null ? HtmlInspectionTarget.empty() : target;
        UiRuntimeInspectionSource runtime = safeTarget.runtimeSource();
        if (runtime == null || !runtime.available()) return HtmlDevToolsSnapshot.empty(safeSession);

        Object rootCandidate = runtime.document().rootOptional().orElse(null);
        if (!(rootCandidate instanceof UiDomElement root)) return HtmlDevToolsSnapshot.empty(safeSession);

        ArrayList<HtmlElementSnapshot> elements = new ArrayList<>();
        HashMap<Integer, UiDomElement> byId = new HashMap<>();
        collect(root, 0, 0, "", runtime, elements, byId);

        int selectedId = safeSession.selectedNodeId() == 0 ? root.nodeId() : safeSession.selectedNodeId();
        UiDomElement selectedElement = byId.getOrDefault(selectedId, root);
        HtmlElementSnapshot selected = elements.stream()
                .filter(item -> item.id() == selectedElement.nodeId())
                .findFirst()
                .orElse(null);

        Map<String, String> computedStyle = selectedElement.computedStyle();
        List<HtmlCssPropertySnapshot> matched = withRuntimeFlags(selectedElement.nodeId(), css.declared(computedStyle));
        List<HtmlCssPropertySnapshot> computed = withRuntimeFlags(selectedElement.nodeId(), css.computed(computedStyle));
        HtmlCssBoxSnapshot box = runtime.layout().box(selectedElement)
                .map(HtmlCssBoxSnapshot::from)
                .orElse(HtmlCssBoxSnapshot.empty());

        return new HtmlDevToolsSnapshot(
                safeSession.selectNode(selectedElement.nodeId()),
                true,
                runtime.viewportWidth(),
                runtime.viewportHeight(),
                elements,
                selected,
                matched,
                computed,
                box,
                new HtmlCodeView(safeTarget.sourcePath(), safeTarget.sourceText()),
                safeTarget.diagnostics(),
                safeTarget.actions(),
                HtmlDevToolsRuntime.canUndo(),
                HtmlDevToolsRuntime.canRedo(),
                HtmlDevToolsRuntime.hasUnsavedChanges()
        );
    }

    private List<HtmlCssPropertySnapshot> withRuntimeFlags(int nodeId, List<HtmlCssPropertySnapshot> rows) {
        if (rows == null || rows.isEmpty()) return List.of();
        ArrayList<HtmlCssPropertySnapshot> out = new ArrayList<>(rows.size());
        for (HtmlCssPropertySnapshot row : rows) {
            boolean disabled = HtmlDevToolsRuntime.styleDisabled(nodeId, row.name());
            out.add(row.withDisabled(disabled, HtmlDevToolsRuntime.styleSnapshotValue(nodeId, row.name(), row.value())));
        }
        return out;
    }

    private void collect(
            UiDomElement element,
            int parentId,
            int depth,
            String parentPath,
            UiRuntimeInspectionSource runtime,
            ArrayList<HtmlElementSnapshot> out,
            Map<Integer, UiDomElement> byId
    ) {
        if (element == null) return;
        String selector = selector(element);
        String path = parentPath == null || parentPath.isBlank() ? selector : parentPath + " > " + selector;
        HtmlCssBoxSnapshot box = runtime.layout().box(element)
                .map(HtmlCssBoxSnapshot::from)
                .orElse(HtmlCssBoxSnapshot.empty());
        out.add(new HtmlElementSnapshot(
                element.nodeId(),
                parentId,
                depth,
                element.tagName(),
                selector,
                path,
                box.x(),
                box.y(),
                box.width(),
                box.height(),
                attributeText(element),
                element.style("display", ""),
                childElementCount(element)
        ));
        byId.put(element.nodeId(), element);
        for (Object child : element.children()) {
            if (child instanceof UiDomElement childElement) {
                collect(childElement, element.nodeId(), depth + 1, path, runtime, out, byId);
            }
        }
    }

    private String attributeText(UiDomElement element) {
        if (element == null || element.attributes().isEmpty()) return "";
        StringJoiner out = new StringJoiner(" ");
        appendAttribute(out, "id", element.attribute("id", ""));
        appendAttribute(out, "class", element.attribute("class", ""));
        for (Map.Entry<String, String> entry : element.attributes().entrySet()) {
            String key = entry.getKey();
            if ("id".equals(key) || "class".equals(key)) continue;
            appendAttribute(out, key, entry.getValue());
        }
        return out.toString();
    }

    private void appendAttribute(StringJoiner out, String key, String value) {
        if (key == null || key.isBlank()) return;
        String cleanValue = value == null ? "" : value.trim();
        if (cleanValue.isBlank()) out.add(key);
        else out.add(key + "=\"" + cleanValue + "\"");
    }

    private int childElementCount(UiDomElement element) {
        if (element == null) return 0;
        int count = 0;
        for (Object child : element.children()) if (child instanceof UiDomElement) count++;
        return count;
    }

    private String selector(UiDomElement element) {
        StringBuilder out = new StringBuilder(element.tagName());
        String id = element.id();
        if (id != null && !id.isBlank()) out.append('#').append(id.trim());
        String classes = element.attribute("class", "");
        if (classes != null && !classes.isBlank()) {
            for (String token : classes.trim().split("\s+")) {
                if (!token.isBlank()) out.append('.').append(token);
            }
        }
        return out.toString();
    }
}
