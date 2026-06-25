package dev.takesome.helix.ui.component.event;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import java.util.LinkedHashMap;
import java.util.Map;

/** Canonical event emitted by retained UI components. */
public record UiComponentEvent(
        String componentType,
        String interaction,
        String nodeId,
        String action,
        String name,
        String value,
        String previousValue,
        String label,
        boolean enabled,
        boolean checked,
        boolean committed,
        boolean open,
        Map<String, String> attributes
) {
    public static final String EVENT_ID = "ui.component.event";

    public UiComponentEvent {
        componentType = clean(componentType);
        interaction = clean(interaction);
        nodeId = clean(nodeId);
        action = clean(action);
        name = clean(name);
        value = emptyIfNull(value);
        previousValue = emptyIfNull(previousValue);
        label = emptyIfNull(label);
        attributes = immutableCopy(attributes);
    }

    public String aliasEventId() {
        if (componentType.isBlank() || interaction.isBlank()) return EVENT_ID;
        return "ui." + componentType + "." + interaction;
    }

    public Map<String, String> arguments() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.put("componentType", componentType);
        out.put("interaction", interaction);
        out.put("nodeId", nodeId);
        out.put("action", action);
        out.put("name", name);
        out.put("value", value);
        out.put("previousValue", previousValue);
        out.put("label", label);
        out.put("enabled", Boolean.toString(enabled));
        out.put("checked", Boolean.toString(checked));
        out.put("committed", Boolean.toString(committed));
        out.put("open", Boolean.toString(open));
        out.putAll(attributes);
        return out;
    }

    public static Builder builder(String componentType, String interaction) {
        return new Builder(componentType, interaction);
    }

    public static final class Builder {
        private final String componentType;
        private final String interaction;
        private String nodeId = "";
        private String action = "";
        private String name = "";
        private String value = "";
        private String previousValue = "";
        private String label = "";
        private boolean enabled = true;
        private boolean checked;
        private boolean committed;
        private boolean open;
        private final LinkedHashMap<String, String> attributes = new LinkedHashMap<>();

        private Builder(String componentType, String interaction) {
            this.componentType = componentType;
            this.interaction = interaction;
        }

        public Builder nodeId(String nodeId) { this.nodeId = clean(nodeId); return this; }
        public Builder action(String action) { this.action = clean(action); return this; }
        public Builder name(String name) { this.name = clean(name); return this; }
        public Builder value(String value) { this.value = emptyIfNull(value); return this; }
        public Builder previousValue(String previousValue) { this.previousValue = emptyIfNull(previousValue); return this; }
        public Builder label(String label) { this.label = emptyIfNull(label); return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder checked(boolean checked) { this.checked = checked; return this; }
        public Builder committed(boolean committed) { this.committed = committed; return this; }
        public Builder open(boolean open) { this.open = open; return this; }

        public Builder attribute(String key, String value) {
            String normalizedKey = clean(key);
            if (!normalizedKey.isBlank() && value != null) attributes.put(normalizedKey, value);
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            if (attributes == null) return this;
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                attribute(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public UiComponentEvent build() {
            return new UiComponentEvent(componentType, interaction, nodeId, action, name, value, previousValue,
                    label, enabled, checked, committed, open, attributes);
        }
    }

    private static String clean(String value) { return trimToEmpty(value); }

    private static Map<String, String> immutableCopy(Map<String, String> input) {
        if (input == null || input.isEmpty()) return Map.of();
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String key = clean(entry.getKey());
            String value = entry.getValue();
            if (!key.isBlank() && value != null) out.put(key, value);
        }
        return Map.copyOf(out);
    }
}
