package dev.takesome.helix.ui.css.properties.layout;

import dev.takesome.helix.ui.css.UiCssDeclaration;
import dev.takesome.helix.ui.css.UiCssLengthPropertySpec;
import dev.takesome.helix.ui.css.UiCssParseContext;
import dev.takesome.helix.ui.css.UiCssShorthandPropertySpec;
import dev.takesome.helix.ui.css.UiCssShorthandSupport;

import java.util.List;
import java.util.Set;

public final class PaddingCssProperty extends UiCssLengthPropertySpec implements UiCssShorthandPropertySpec {
    public PaddingCssProperty() {
        super("padding", Set.of(), true);
    }

    public List<UiCssDeclaration> expand(UiCssParseContext context, String rawValue) {
        List<String> tokens = UiCssShorthandSupport.tokens(rawValue);
        if (tokens.isEmpty()) return List.of();
        String top = tokens.get(0);
        String right = tokens.size() > 1 ? tokens.get(1) : top;
        String bottom = tokens.size() > 2 ? tokens.get(2) : top;
        String left = tokens.size() > 3 ? tokens.get(3) : right;
        return List.of(
                new UiCssDeclaration("padding-top", top),
                new UiCssDeclaration("padding-right", right),
                new UiCssDeclaration("padding-bottom", bottom),
                new UiCssDeclaration("padding-left", left)
        );
    }
}
