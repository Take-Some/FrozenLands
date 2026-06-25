package dev.takesome.helix.ui.markup.internal.factory;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UiMarkupElementComposerRegistry {
    private final Map<String, UiMarkupElementComposer> composers = new LinkedHashMap<>();

    public UiMarkupElementComposerRegistry register(UiMarkupElementComposer composer) {
        composers.put(composer.id(), composer);
        return this;
    }

    public UiMarkupElementComposer require(String id) {
        UiMarkupElementComposer composer = find(id);
        if (composer == null) throw new UiMarkupFactoryException("Unknown markup composer: " + id);
        return composer;
    }

    public UiMarkupElementComposer find(String id) {
        if (id == null || id.isBlank()) return null;
        return composers.get(id);
    }
}
