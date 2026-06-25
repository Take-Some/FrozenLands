package dev.takesome.helix.ui.css;


import static dev.takesome.helix.validation.EngineValidator.lowerTrimToEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small tokenizer helpers for CSS shorthand definition files. */
public final class UiCssShorthandSupport {
    private UiCssShorthandSupport() {
    }

    public static List<String> tokens(String raw) {
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
            if (ch == '(') {
                depth++;
                current.append(ch);
                continue;
            }
            if (ch == ')' && depth > 0) {
                depth--;
                current.append(ch);
                continue;
            }
            if (Character.isWhitespace(ch) && depth == 0) {
                flush(current, out);
                continue;
            }
            current.append(ch);
        }
        flush(current, out);
        return out;
    }

    public static String lower(String value) {
        return lowerTrimToEmpty(value, Locale.ROOT);
    }

    public static boolean lengthLike(String value) {
        String v = lower(value);
        if (v.isBlank()) return false;
        if (v.endsWith("px") || v.endsWith("%") || v.endsWith("em") || v.endsWith("rem")) return true;
        return v.matches("[-+]?[0-9]*\\.?[0-9]+");
    }

    public static boolean colorLike(String value) {
        String v = lower(value);
        return v.startsWith("#") || v.startsWith("rgb(") || v.startsWith("rgba(") || "transparent".equals(v) || "white".equals(v) || "black".equals(v) || "red".equals(v) || "green".equals(v) || "blue".equals(v) || "yellow".equals(v) || "orange".equals(v) || "purple".equals(v) || "gray".equals(v) || "grey".equals(v);
    }

    private static void flush(StringBuilder current, List<String> out) {
        String value = current.toString().trim();
        if (!value.isBlank()) out.add(value);
        current.setLength(0);
    }
}
