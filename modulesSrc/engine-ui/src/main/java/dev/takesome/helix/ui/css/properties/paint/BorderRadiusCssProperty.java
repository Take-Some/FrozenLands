package dev.takesome.helix.ui.css.properties.paint;

import dev.takesome.helix.ui.css.UiCssDeclaration;
import dev.takesome.helix.ui.css.UiCssParseContext;
import dev.takesome.helix.ui.css.UiCssShorthandPropertySpec;
import dev.takesome.helix.ui.css.UiCssShorthandSupport;
import dev.takesome.helix.ui.css.UiCssStringPropertySpec;

import java.util.List;
import java.util.Set;

public final class BorderRadiusCssProperty extends UiCssStringPropertySpec implements UiCssShorthandPropertySpec {
    public BorderRadiusCssProperty() {
        super("border-radius", Set.of(), true);
    }

    public List<UiCssDeclaration> expand(UiCssParseContext context, String rawValue) {
        List<String> tokens = UiCssShorthandSupport.tokens(rawValue);
        if (tokens.isEmpty()) return List.of();
        String topLeft = tokens.get(0);
        String topRight = tokens.size() > 1 ? tokens.get(1) : topLeft;
        String bottomRight = tokens.size() > 2 ? tokens.get(2) : topLeft;
        String bottomLeft = tokens.size() > 3 ? tokens.get(3) : topRight;
        return List.of(
                new UiCssDeclaration("border-top-left-radius", topLeft),
                new UiCssDeclaration("border-top-right-radius", topRight),
                new UiCssDeclaration("border-bottom-right-radius", bottomRight),
                new UiCssDeclaration("border-bottom-left-radius", bottomLeft)
        );
    }
}
