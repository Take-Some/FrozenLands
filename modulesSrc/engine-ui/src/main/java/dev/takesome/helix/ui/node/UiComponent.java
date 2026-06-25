package dev.takesome.helix.ui.node;

import dev.takesome.helix.ui.component.UiComponentId;
import dev.takesome.helix.ui.component.UiComponentPropertyBag;
import dev.takesome.helix.ui.component.UiComponentPropertyKey;
import dev.takesome.helix.ui.component.UiComponentTypeId;
import dev.takesome.helix.ui.css.runtime.UiCssStyleRuntimeController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unified parent for all retained runtime UI elements.
 *
 * <p>Concrete controls are component types, not separate root models:
 * button, label, image, panel, slider, combo_box, input_capture, etc.</p>
 *
 * <p>The class is intentionally placed above existing retained nodes, so CSS/HTML
 * style and transition runtime can be attached to every component uniformly.</p>
 */
public abstract class UiComponent extends Node {
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private UiComponentId componentId = UiComponentId.generated(SEQUENCE.incrementAndGet());
    private UiComponentTypeId componentType = UiComponentTypeId.of(inferTypeName(getClass().getSimpleName()));
    private final UiComponentPropertyBag properties = new UiComponentPropertyBag();

    private Map<String, String> cssStyle = Map.of();
    private Map<String, Map<String, String>> cssStateStyles = Map.of();
    private UiCssStyleRuntimeController cssRuntime;

    public final UiComponentId componentId() {
        return componentId;
    }

    public final void setComponentId(UiComponentId componentId) {
        if (componentId != null) this.componentId = componentId;
    }

    public final void setComponentId(String componentId) {
        if (componentId != null && !componentId.isBlank()) this.componentId = UiComponentId.of(componentId);
    }

    public final UiComponentTypeId componentType() {
        return componentType;
    }

    public final void setComponentType(UiComponentTypeId componentType) {
        if (componentType != null) this.componentType = componentType;
    }

    public final void setComponentType(String componentType) {
        if (componentType != null && !componentType.isBlank()) this.componentType = UiComponentTypeId.of(componentType);
    }

    public final UiComponentPropertyBag properties() {
        return properties;
    }

    public final <T> void setProperty(UiComponentPropertyKey<T> key, T value) {
        properties.set(key, value);
        onComponentPropertyChanged(key.name(), value);
        markDirty();
    }

    public final void setPropertyRaw(String name, Object value) {
        properties.setRaw(name, value);
        onComponentPropertyChanged(name, value);
        markDirty();
    }

    /**
     * Extension hook for native components that mirror generic component properties
     * into strongly typed runtime state, for example checked/value/min/max.
     */
    protected void onComponentPropertyChanged(String name, Object value) {
    }

    public final Optional<Object> property(String name) {
        return properties.getRaw(name);
    }

    public final Map<String, String> cssStyle() {
        return cssStyle;
    }

    public final Map<String, Map<String, String>> cssStateStyles() {
        return cssStateStyles;
    }

    public final UiCssStyleRuntimeController cssRuntime() {
        return cssRuntime;
    }

    /**
     * Installs HTML/CSS-like style as a runtime binding. Transition properties inside
     * the style are resolved by UiCssStyleRuntimeController and are updated every frame.
     */
    public final UiCssStyleRuntimeController installCssStyle(Map<String, String> targetStyle) {
        return installCssStyle(componentId.value(), targetStyle, Map.of());
    }

    public final UiCssStyleRuntimeController installCssStyle(
            String runtimeKey,
            Map<String, String> targetStyle,
            Map<String, Map<String, String>> stateStyles
    ) {
        cssStyle = copyStyle(targetStyle);
        cssStateStyles = copyStateStyles(stateStyles);

        if (cssRuntime != null) {
            removeRuntimeBinding(cssRuntime);
        }

        cssRuntime = new UiCssStyleRuntimeController(
                runtimeKey == null || runtimeKey.isBlank() ? componentId.value() : runtimeKey,
                cssStyle,
                cssStateStyles
        );
        cssRuntime.applyInitial(this);
        addRuntimeBinding(cssRuntime);
        markDirty();
        return cssRuntime;
    }

    public final void updateCssStyle(Map<String, String> targetStyle) {
        cssStyle = copyStyle(targetStyle);
        if (cssRuntime == null) {
            installCssStyle(componentId.value(), cssStyle, cssStateStyles);
            return;
        }
        cssRuntime.setTargetStyle(cssStyle);
        markDirty();
    }

    public final void updateCssStateStyles(Map<String, Map<String, String>> stateStyles) {
        cssStateStyles = copyStateStyles(stateStyles);
        if (cssRuntime == null) {
            installCssStyle(componentId.value(), cssStyle, cssStateStyles);
            return;
        }
        cssRuntime.setStateStyles(cssStateStyles);
        markDirty();
    }

    private static Map<String, String> copyStyle(Map<String, String> style) {
        if (style == null || style.isEmpty()) return Map.of();
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        style.forEach((key, value) -> {
            if (key != null && value != null) out.put(key, value);
        });
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Map<String, String>> copyStateStyles(Map<String, Map<String, String>> stateStyles) {
        if (stateStyles == null || stateStyles.isEmpty()) return Map.of();
        LinkedHashMap<String, Map<String, String>> out = new LinkedHashMap<>();
        stateStyles.forEach((state, style) -> {
            if (state != null && style != null) out.put(state, copyStyle(style));
        });
        return Collections.unmodifiableMap(out);
    }

    private static String inferTypeName(String simpleName) {
        String name = simpleName == null || simpleName.isBlank() ? "component" : simpleName;
        if (name.startsWith("Ui") && name.length() > 2) name = name.substring(2);
        if (name.endsWith("Node") && name.length() > 4) name = name.substring(0, name.length() - 4);
        if (name.endsWith("Component") && name.length() > 9) name = name.substring(0, name.length() - 9);

        StringBuilder out = new StringBuilder(name.length() + 8);
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) out.append('_');
                out.append(Character.toLowerCase(ch));
            } else if (ch == '-' || ch == ' ') {
                out.append('_');
            } else {
                out.append(ch);
            }
        }
        String type = out.toString().replaceAll("_+", "_");
        return type.isBlank() ? "component" : type;
    }
}
