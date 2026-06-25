package dev.takesome.helix.ui.html;

import java.util.Set;

public interface UiHtmlTagSpec {
    String name();
    Set<String> aliases();
    String composerId();
    Set<String> allowedAttributes();
    default UiHtmlTagMeta meta() { return UiHtmlTagMeta.inferred(this); }
    default UiHtmlDefinitionStatus status() { return UiHtmlDefinitionStatus.STABLE; }
    default String replacement() { return ""; }
}
