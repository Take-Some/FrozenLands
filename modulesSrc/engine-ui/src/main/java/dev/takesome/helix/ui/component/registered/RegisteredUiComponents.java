package dev.takesome.helix.ui.component.registered;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import static dev.takesome.helix.validation.EngineValidator.toStringOrEmpty;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.component.UiComponentChildrenPolicy;
import dev.takesome.helix.ui.component.UiComponentDefinition;
import dev.takesome.helix.ui.component.UiComponentException;
import dev.takesome.helix.ui.component.UiComponentInstantiator;
import dev.takesome.helix.ui.component.UiComponentRegistry;
import dev.takesome.helix.ui.component.UiComponentTypeId;
import dev.takesome.helix.ui.uiComponents.button.UiButtonNode;
import dev.takesome.helix.ui.uiComponents.checkbox.UiCheckboxNode;
import dev.takesome.helix.ui.uiComponents.combo.UiComboBoxNode;
import dev.takesome.helix.ui.uiComponents.combo.UiComboBoxOption;
import dev.takesome.helix.ui.uiComponents.image.UiImageNode;
import dev.takesome.helix.ui.uiComponents.input.UiInputCaptureNode;
import dev.takesome.helix.ui.uiComponents.input.UiInputNode;
import dev.takesome.helix.ui.uiComponents.label.UiLabelNode;
import dev.takesome.helix.ui.uiComponents.panel.UiPanelNode;
import dev.takesome.helix.ui.uiComponents.slider.UiSliderNode;
import dev.takesome.helix.ui.uiComponents.text.UiTextNode;
import dev.takesome.helix.ui.node.UiComponent;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single package-owned registry of all built-in engine-ui components.
 *
 * <p>New engine components should be added here first, then exposed through HTML/Lua/data composers.</p>
 */
public final class RegisteredUiComponents {
    private static final Set<String> COMMON = Set.of(
            "id", "class", "style", "title",
            "x", "y", "w", "h", "width", "height",
            "left", "top", "display", "visibility", "opacity",
            "transition", "transition-property", "transition-duration", "transition-delay", "transition-timing-function",
            "action", "command", "data-*", "data-action", "data-target-id",
            "bind", "bind-text", "bind-value", "bind-visible", "bind-class", "bind-style"
    );

    private RegisteredUiComponents() {
    }

    public static UiComponentRegistry newRegistry() {
        UiComponentRegistry registry = new UiComponentRegistry();
        registerDefaults(registry);
        return registry;
    }

    public static void registerDefaults(UiComponentRegistry registry) {
        if (registry == null) throw new UiComponentException("UiComponentRegistry is required");
        registry.register(button());
        registry.register(label());
        registry.register(text());
        registry.register(image());
        registry.register(panel());
        registry.register(checkbox());
        registry.register(slider());
        registry.register(comboBox());
        registry.register(input());
        registry.register(inputCapture());
    }

    public static UiComponentDefinition button() {
        return definition(RegisteredUiComponentIds.BUTTON, UiComponentChildrenPolicy.ALLOW,
                Set.of("text", "value", "disabled"),
                descriptor -> new UiButtonNode(string(descriptor.properties(), "text", string(descriptor.properties(), "value", ""))));
    }

    public static UiComponentDefinition label() {
        return definition(RegisteredUiComponentIds.LABEL, UiComponentChildrenPolicy.NONE,
                Set.of("text", "value", "font", "font-id", "scale", "align"),
                descriptor -> new UiLabelNode(string(descriptor.properties(), "text", string(descriptor.properties(), "value", ""))));
    }

    public static UiComponentDefinition text() {
        return definition(RegisteredUiComponentIds.TEXT, UiComponentChildrenPolicy.NONE,
                Set.of("text", "value", "font", "font-id", "scale", "align"),
                descriptor -> new UiTextNode(string(descriptor.properties(), "text", string(descriptor.properties(), "value", ""))));
    }

    public static UiComponentDefinition image() {
        return definition(RegisteredUiComponentIds.IMAGE, UiComponentChildrenPolicy.NONE,
                Set.of("src", "alt", "value", "title"),
                descriptor -> new UiImageNode(string(descriptor.properties(), "src", string(descriptor.properties(), "value", ""))));
    }

    public static UiComponentDefinition panel() {
        return definition(RegisteredUiComponentIds.PANEL, UiComponentChildrenPolicy.ALLOW,
                Set.of("background", "background-color", "border", "border-color", "border-width", "box-shadow"),
                descriptor -> new UiPanelNode(UiColor.TRANSPARENT));
    }

    public static UiComponentDefinition checkbox() {
        return definition(RegisteredUiComponentIds.CHECKBOX, UiComponentChildrenPolicy.NONE,
                Set.of("text", "label", "value", "checked", "disabled", "box-size", "label-gap", "font", "font-id"),
                descriptor -> {
                    Map<String, Object> properties = descriptor.properties();
                    return new UiCheckboxNode(
                            string(properties, "label", string(properties, "text", string(properties, "value", ""))),
                            bool(properties, "checked", bool(properties, "value", false))
                    );
                });
    }

    public static UiComponentDefinition slider() {
        return definition(RegisteredUiComponentIds.SLIDER, UiComponentChildrenPolicy.NONE,
                Set.of("value", "min", "max", "step", "disabled", "show-value"),
                descriptor -> {
                    Map<String, Object> properties = descriptor.properties();
                    double min = number(properties, "min", 0.0);
                    double max = number(properties, "max", 100.0);
                    double step = number(properties, "step", 1.0);
                    double value = number(properties, "value", min);
                    return new UiSliderNode(min, max, step, value);
                });
    }

    public static UiComponentDefinition comboBox() {
        return definition(RegisteredUiComponentIds.COMBO_BOX, UiComponentChildrenPolicy.ALLOW,
                Set.of("value", "name", "disabled", "options", "closed-icon", "open-icon", "icon", "icon-color"),
                descriptor -> {
                    UiComboBoxNode comboBox = new UiComboBoxNode(options(descriptor.properties().get("options")), string(descriptor.properties(), "value", ""));
                    String closedIcon = string(descriptor.properties(), "closed-icon", string(descriptor.properties(), "icon", ""));
                    String openIcon = string(descriptor.properties(), "open-icon", "");
                    if (!closedIcon.isBlank() || !openIcon.isBlank()) comboBox.setIconIds(closedIcon, openIcon);
                    return comboBox;
                });
    }

    public static UiComponentDefinition input() {
        return definition(RegisteredUiComponentIds.INPUT, UiComponentChildrenPolicy.NONE,
                Set.of("value", "placeholder", "name", "disabled", "readonly", "required", "max-length"),
                descriptor -> new UiInputNode(string(descriptor.properties(), "value", "")));
    }

    public static UiComponentDefinition inputCapture() {
        return definition(RegisteredUiComponentIds.INPUT_CAPTURE, UiComponentChildrenPolicy.NONE,
                Set.of("value", "placeholder", "listening-text", "input-action", "device", "disabled"),
                descriptor -> new UiInputCaptureNode(string(descriptor.properties(), "value", "")));
    }

    private static UiComponentDefinition definition(
            UiComponentTypeId type,
            UiComponentChildrenPolicy childrenPolicy,
            Set<String> properties,
            UiComponentInstantiator instantiator
    ) {
        LinkedHashSet<String> supported = new LinkedHashSet<>(COMMON);
        if (properties != null) supported.addAll(properties);
        return new UiComponentDefinition(type, childrenPolicy, supported, descriptor -> {
            UiComponent component = instantiator.create(descriptor);
            component.setComponentType(type);
            return component;
        });
    }

    private static String string(Map<String, Object> properties, String key, String fallback) {
        Object value = properties.get(key);
        if (value == null) return emptyIfNull(fallback);
        return value.toString();
    }

    private static boolean bool(Map<String, Object> properties, String key, boolean fallback) {
        Object value = properties.get(key);
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0.0;
        if (value instanceof String s) {
            String normalized = s.trim();
            if (normalized.isEmpty()) return true;
            if ("true".equalsIgnoreCase(normalized) || "checked".equalsIgnoreCase(normalized) || "1".equals(normalized)) return true;
            if ("false".equalsIgnoreCase(normalized) || "0".equals(normalized)) return false;
        }
        return fallback;
    }

    private static double number(Map<String, Object> properties, String key, double fallback) {
        Object value = properties.get(key);
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static List<UiComboBoxOption> options(Object raw) {
        if (!(raw instanceof Collection<?> collection) || collection.isEmpty()) return List.of();
        return collection.stream()
                .map(RegisteredUiComponents::option)
                .toList();
    }

    private static UiComboBoxOption option(Object raw) {
        if (raw instanceof UiComboBoxOption option) return option;
        if (raw instanceof Map<?, ?> map) {
            Object value = map.get("value");
            Object label = map.get("label");
            Object disabled = map.get("disabled");
            String resolvedValue = toStringOrEmpty(value);
            String resolvedLabel = label == null ? resolvedValue : label.toString();
            return new UiComboBoxOption(resolvedValue, resolvedLabel, disabled instanceof Boolean b && b);
        }
        String value = toStringOrEmpty(raw);
        return new UiComboBoxOption(value, value, false);
    }
}
