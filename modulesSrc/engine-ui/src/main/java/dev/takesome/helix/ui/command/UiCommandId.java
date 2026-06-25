package dev.takesome.helix.ui.command;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import java.util.Objects;
import java.util.regex.Pattern;

/** Typed, validated command id used by UI actions. */
public final class UiCommandId {
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_.:-]+");

    private final String value;

    public UiCommandId(String value) {
        String normalized = trimToEmpty(value);
        if (normalized.isBlank()) throw new IllegalArgumentException("UI command id must not be blank");
        if (!VALID.matcher(normalized).matches()) throw new IllegalArgumentException("Invalid UI command id: " + normalized);
        this.value = normalized;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UiCommandId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
