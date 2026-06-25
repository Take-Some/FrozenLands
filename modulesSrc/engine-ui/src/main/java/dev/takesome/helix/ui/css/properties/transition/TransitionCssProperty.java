package dev.takesome.helix.ui.css.properties.transition;

import dev.takesome.helix.ui.css.UiCssDeclaration;
import dev.takesome.helix.ui.css.UiCssParseContext;
import dev.takesome.helix.ui.css.UiCssShorthandPropertySpec;
import dev.takesome.helix.ui.css.UiCssShorthandSupport;
import dev.takesome.helix.ui.css.UiCssStringPropertySpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TransitionCssProperty extends UiCssStringPropertySpec implements UiCssShorthandPropertySpec {
    private static final Set<String> TIMING = Set.of("ease", "linear", "ease-in", "ease-out", "ease-in-out", "step-start", "step-end");

    public TransitionCssProperty() {
        super("transition", Set.of(), true);
    }

    public List<UiCssDeclaration> expand(UiCssParseContext context, String rawValue) {
        ArrayList<String> properties = new ArrayList<>();
        ArrayList<String> durations = new ArrayList<>();
        ArrayList<String> delays = new ArrayList<>();
        ArrayList<String> timings = new ArrayList<>();

        for (String group : groups(rawValue)) {
            Parts parts = parts(group);
            properties.add(parts.property.isBlank() ? "all" : parts.property);
            durations.add(parts.duration.isBlank() ? "0ms" : parts.duration);
            delays.add(parts.delay.isBlank() ? "0ms" : parts.delay);
            timings.add(parts.timing.isBlank() ? "ease" : parts.timing);
        }

        if (properties.isEmpty()) return List.of();
        return List.of(
                new UiCssDeclaration("transition-property", String.join(", ", properties)),
                new UiCssDeclaration("transition-duration", String.join(", ", durations)),
                new UiCssDeclaration("transition-delay", String.join(", ", delays)),
                new UiCssDeclaration("transition-timing-function", String.join(", ", timings))
        );
    }

    private Parts parts(String rawValue) {
        String property = "";
        String duration = "";
        String delay = "";
        String timing = "";
        for (String token : UiCssShorthandSupport.tokens(rawValue)) {
            String lower = UiCssShorthandSupport.lower(token);
            if (isTime(lower)) {
                if (duration.isBlank()) duration = token;
                else if (delay.isBlank()) delay = token;
            } else if (timing.isBlank() && (TIMING.contains(lower) || lower.startsWith("cubic-bezier(") || lower.startsWith("steps("))) {
                timing = token;
            } else if (property.isBlank()) {
                property = token;
            }
        }
        return new Parts(property, duration, delay, timing);
    }

    private List<String> groups(String raw) {
        ArrayList<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        StringBuilder current = new StringBuilder();
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (quote != 0) {
                current.append(ch);
                if (ch == quote) quote = 0;
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                current.append(ch);
                continue;
            }
            if (ch == '(') depth++;
            if (ch == ')' && depth > 0) depth--;
            if (ch == ',' && depth == 0) {
                add(out, current);
                continue;
            }
            current.append(ch);
        }
        add(out, current);
        return out;
    }

    private void add(List<String> out, StringBuilder current) {
        String value = current.toString().trim();
        if (!value.isBlank()) out.add(value);
        current.setLength(0);
    }

    private boolean isTime(String value) {
        return value.endsWith("ms") || value.endsWith("s");
    }

    private record Parts(String property, String duration, String delay, String timing) { }
}
