package dev.takesome.helix.ui.markup.internal.factory;

import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.internal.action.UiMarkupActionBinder;
import dev.takesome.helix.ui.markup.internal.action.UiMarkupComponentEventBridge;
import dev.takesome.helix.ui.markup.internal.layout.UiDomLayoutResolver;
import dev.takesome.helix.ui.markup.internal.model.UiDomAttributes;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.uiComponents.checkbox.UiCheckboxNode;
import dev.takesome.helix.ui.uiComponents.combo.UiComboBoxNode;
import dev.takesome.helix.ui.uiComponents.combo.UiComboBoxOption;
import dev.takesome.helix.ui.uiComponents.input.UiInputCaptureNode;
import dev.takesome.helix.ui.uiComponents.input.UiInputNode;
import dev.takesome.helix.ui.uiComponents.slider.UiSliderNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UiMarkupInputFactory {
    private final UiMarkupActionBinder actions;
    private final UiDomStyleReader reader;
    private final UiDomLayoutResolver layout;
    private final UiBindingSource bindingSource;

    public UiMarkupInputFactory(UiDomStyleReader reader, UiDomLayoutResolver layout) {
        this(null, reader, layout);
    }

    public UiMarkupInputFactory(UiMarkupActionBinder actions, UiDomStyleReader reader, UiDomLayoutResolver layout) {
        this(actions, reader, layout, null);
    }

    public UiMarkupInputFactory(UiMarkupActionBinder actions, UiDomStyleReader reader, UiDomLayoutResolver layout, UiBindingSource bindingSource) {
        this.actions = actions;
        this.reader = reader;
        this.layout = layout;
        this.bindingSource = bindingSource;
    }

    public Node input(UiDomElement element, Map<String, String> style, float parentW, float parentH) {
        String bindValue = reader.first(style, "bind-value");
        UiInputNode node = new UiInputNode(!bindValue.isBlank() && bindingSource != null ? bindingSource.text(bindValue) : reader.value(style, "value"));
        if (!bindValue.isBlank() && bindingSource != null) {
            node.addRuntimeBinding((boundNode, dt) -> node.setValue(bindingSource.text(bindValue), false));
        }
        node.setEventSink(new UiMarkupComponentEventBridge(actions, style));
        node.setPlaceholder(reader.value(style, "placeholder"));
        node.setFontId(reader.font(style));
        if (reader.has(style, "max-length")) node.setMaxLength(reader.integer(style, "max-length", 96));
        if (reader.has(style, "font-size")) node.setFontScale(Math.max(0.01f, reader.number(style, "font-size", 16f) / 16f));
        else if (reader.has(style, "scale")) node.setFontScale(reader.number(style, "scale", 1f));
        layout.setBounds(node, style, parentW, parentH, 320f, 48f);
        layout.applyState(node, style);
        return node;
    }

    public Node checkbox(UiDomElement element, Map<String, String> style, float parentW, float parentH, UiDomRetainedNodeFactory nodes) {
        UiCheckboxNode node = new UiCheckboxNode(
                firstText(element, style),
                reader.bool(style, "checked", reader.bool(style, "value", false))
        );
        node.setNodeId(reader.value(style, "id"));
        node.setName(reader.first(style, "name", "data-setting", "setting", "settings-key"));
        node.setFontId(reader.first(style, "font-id", "font"));
        node.setEventSink(new UiMarkupComponentEventBridge(actions, style));
        if (reader.has(style, "box-size")) node.setBoxSize(reader.number(style, "box-size", 32f));
        if (reader.has(style, "label-gap")) node.setLabelGap(reader.number(style, "label-gap", 10f));
        layout.setBounds(node, style, parentW, parentH, 320f, 42f);
        compileOverlayChildren(nodes, node, element);
        layout.applyState(node, style);
        return node;
    }

    public Node slider(UiDomElement element, Map<String, String> style, float parentW, float parentH, UiDomRetainedNodeFactory nodes) {
        float min = reader.number(style, "min", 0f);
        float max = reader.number(style, "max", 100f);
        float step = reader.number(style, "step", 1f);
        float value = reader.number(style, "value", min);
        UiSliderNode node = new UiSliderNode(min, max, step, value);
        node.setEventSink(new UiMarkupComponentEventBridge(actions, style));
        node.setNodeId(reader.value(style, "id"));
        node.setName(reader.first(style, "name", "data-setting", "setting", "settings-key"));
        layout.setBounds(node, style, parentW, parentH, 320f, 42f);
        compileOverlayChildren(nodes, node, element);
        layout.applyState(node, style);
        return node;
    }

    public Node comboBox(UiDomElement element, Map<String, String> style, float parentW, float parentH, UiDomRetainedNodeFactory nodes) {
        UiComboBoxNode node = new UiComboBoxNode(options(element), initialComboValue(element, style));
        node.setEventSink(new UiMarkupComponentEventBridge(actions, style));
        node.setNodeId(reader.value(style, "id"));
        node.setName(reader.first(style, "name", "data-setting", "setting", "settings-key"));
        String closedIcon = reader.first(style, "closed-icon", "icon", "data-closed-icon");
        String openIcon = reader.first(style, "open-icon", "icon-open", "data-open-icon");
        if (!closedIcon.isBlank() || !openIcon.isBlank()) node.setIconIds(closedIcon, openIcon);
        layout.setBounds(node, style, parentW, parentH, 320f, 42f);
        if (nodes != null && element != null) nodes.compileChildren(node, element, node.bounds().w, node.bounds().h, "option");
        layout.applyState(node, style);
        return node;
    }

    public Node inputCapture(UiDomElement element, Map<String, String> style, float parentW, float parentH) {
        UiInputCaptureNode node = new UiInputCaptureNode(reader.value(style, "value"));
        node.setEventSink(new UiMarkupComponentEventBridge(actions, style));
        node.setNodeId(reader.value(style, "id"));
        node.setPlaceholder(reader.value(style, "placeholder"));
        node.setListeningText(reader.first(style, "listening-text", "listeningText"));
        node.setInputActionId(reader.first(style, "data-input-action", "input-action", "inputAction"));
        node.setDevice(reader.first(style, "data-device", "device"));
        layout.setBounds(node, style, parentW, parentH, 220f, 42f);
        layout.applyState(node, style);
        return node;
    }

    private void compileOverlayChildren(UiDomRetainedNodeFactory nodes, Node node, UiDomElement element) {
        if (nodes == null || node == null || element == null || element.childCount() == 0) return;
        nodes.compileChildren(node, element, node.bounds().w, node.bounds().h);
    }

    private String firstText(UiDomElement element, Map<String, String> style) {
        String label = reader.first(style, "label", "text");
        if (!label.isBlank()) return label;
        return UiDomAttributes.textOrValue(element, "text");
    }

    private List<UiComboBoxOption> options(UiDomElement element) {
        ArrayList<UiComboBoxOption> out = new ArrayList<>();
        if (element == null) return out;
        for (var child : element.children()) {
            if (!(child instanceof UiDomElement option) || !"option".equalsIgnoreCase(option.tagName())) continue;
            String value = UiDomAttributes.value(option, "value").trim();
            String label = UiDomAttributes.textOrValue(option, "label").trim();
            if (value.isBlank()) value = label;
            if (label.isBlank()) label = value;
            out.add(new UiComboBoxOption(value, label, UiDomAttributes.has(option, "disabled")));
        }
        return out;
    }

    private String initialComboValue(UiDomElement element, Map<String, String> style) {
        String value = reader.value(style, "value");
        if (!value.isBlank()) return value;
        if (element != null) {
            for (var child : element.children()) {
                if (child instanceof UiDomElement option && "option".equalsIgnoreCase(option.tagName()) && UiDomAttributes.has(option, "selected")) {
                    return UiDomAttributes.value(option, "value");
                }
            }
        }
        return "";
    }
}
