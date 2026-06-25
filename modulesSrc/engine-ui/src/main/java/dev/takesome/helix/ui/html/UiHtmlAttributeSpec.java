package dev.takesome.helix.ui.html;

public interface UiHtmlAttributeSpec {
    String name();

    default UiHtmlDefinitionStatus status() { return UiHtmlDefinitionStatus.STABLE; }

    default String replacement() { return ""; }
}
