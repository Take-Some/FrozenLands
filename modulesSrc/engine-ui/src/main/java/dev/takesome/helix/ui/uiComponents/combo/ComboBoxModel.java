package dev.takesome.helix.ui.uiComponents.combo;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import static dev.takesome.helix.validation.EngineValidator.textOrEmpty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Options and selected value for a retained combo box. */
final class ComboBoxModel {
    private final List<UiComboBoxOption> options = new ArrayList<>();
    private String value = "";

    ComboBoxModel(List<UiComboBoxOption> options, String value) {
        if (options != null) this.options.addAll(options);
        String initial = trimToEmpty(value);
        if (initial.isBlank()) {
            UiComboBoxOption first = firstEnabled();
            initial = textOrEmpty(first, item -> item.value());
        }
        this.value = initial;
    }

    String value() { return value; }
    List<UiComboBoxOption> options() { return Collections.unmodifiableList(options); }
    UiComboBoxOption option(int index) { return index >= 0 && index < options.size() ? options.get(index) : null; }
    int size() { return options.size(); }

    boolean setValue(String nextValue) {
        String next = normalizedValue(nextValue);
        if (value.equals(next)) return false;
        value = next;
        return true;
    }

    String normalizedValue(String nextValue) {
        String next = trimToEmpty(nextValue);
        UiComboBoxOption selected = optionByValue(next);
        if (selected == null && !options.isEmpty()) {
            UiComboBoxOption first = firstEnabled();
            next = textOrEmpty(first, item -> item.value());
        }
        return next;
    }

    String labelFor(String value) {
        UiComboBoxOption option = optionByValue(value);
        return option == null ? (emptyIfNull(value)) : option.label();
    }

    String selectedLabel() { return labelFor(value); }

    UiComboBoxOption firstEnabled() {
        for (UiComboBoxOption option : options) if (!option.disabled()) return option;
        return null;
    }

    UiComboBoxOption optionByValue(String value) {
        String wanted = trimToEmpty(value);
        for (UiComboBoxOption option : options) if (option.value().equals(wanted)) return option;
        return null;
    }
}
