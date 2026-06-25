package dev.takesome.helix.ui.component;

/** Common HTML-like/UI properties shared by the component runtime. */
public final class UiComponentProperties {
    public static final UiComponentPropertyKey<String> ID = UiComponentPropertyKey.of("id", String.class);
    public static final UiComponentPropertyKey<String> CLASS = UiComponentPropertyKey.of("class", String.class);
    public static final UiComponentPropertyKey<String> STYLE = UiComponentPropertyKey.of("style", String.class);

    public static final UiComponentPropertyKey<String> TEXT = UiComponentPropertyKey.of("text", String.class);
    public static final UiComponentPropertyKey<String> SRC = UiComponentPropertyKey.of("src", String.class);
    public static final UiComponentPropertyKey<String> ALT = UiComponentPropertyKey.of("alt", String.class);
    public static final UiComponentPropertyKey<String> TITLE = UiComponentPropertyKey.of("title", String.class);

    public static final UiComponentPropertyKey<String> ACTION = UiComponentPropertyKey.of("action", String.class);
    public static final UiComponentPropertyKey<String> COMMAND = UiComponentPropertyKey.of("command", String.class);
    public static final UiComponentPropertyKey<Boolean> DISABLED = UiComponentPropertyKey.of("disabled", Boolean.class);
    public static final UiComponentPropertyKey<Boolean> CHECKED = UiComponentPropertyKey.of("checked", Boolean.class);

    public static final UiComponentPropertyKey<Double> VALUE = UiComponentPropertyKey.of("value", Double.class);
    public static final UiComponentPropertyKey<Double> MIN = UiComponentPropertyKey.of("min", Double.class);
    public static final UiComponentPropertyKey<Double> MAX = UiComponentPropertyKey.of("max", Double.class);
    public static final UiComponentPropertyKey<Double> STEP = UiComponentPropertyKey.of("step", Double.class);

    private UiComponentProperties() {
    }
}
