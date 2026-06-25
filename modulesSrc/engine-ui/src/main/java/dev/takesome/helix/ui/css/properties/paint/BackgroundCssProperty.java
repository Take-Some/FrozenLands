package dev.takesome.helix.ui.css.properties.paint;

import dev.takesome.helix.ui.css.UiCssDeclaration;
import dev.takesome.helix.ui.css.UiCssParseContext;
import dev.takesome.helix.ui.css.UiCssShorthandPropertySpec;
import dev.takesome.helix.ui.css.UiCssShorthandSupport;
import dev.takesome.helix.ui.css.UiCssStringPropertySpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class BackgroundCssProperty extends UiCssStringPropertySpec implements UiCssShorthandPropertySpec {
    private static final Set<String> REPEAT = Set.of("repeat", "repeat-x", "repeat-y", "no-repeat", "space", "round");
    private static final Set<String> POSITION = Set.of("left", "right", "top", "bottom", "center");

    public BackgroundCssProperty() {
        super("background", Set.of(), true);
    }

    public List<UiCssDeclaration> expand(UiCssParseContext context, String rawValue) {
        ArrayList<UiCssDeclaration> out = new ArrayList<>();
        List<String> tokens = UiCssShorthandSupport.tokens(rawValue);
        if (tokens.isEmpty()) return out;

        String image = "";
        String color = "";
        String repeat = "";
        ArrayList<String> position = new ArrayList<>();
        ArrayList<String> size = new ArrayList<>();
        boolean readingSize = false;

        for (String token : tokens) {
            String lower = UiCssShorthandSupport.lower(token);
            if ("/".equals(lower)) {
                readingSize = true;
                continue;
            }
            if (lower.startsWith("url(")) {
                image = token;
            } else if (repeat.isBlank() && REPEAT.contains(lower)) {
                repeat = token;
            } else if (color.isBlank() && UiCssShorthandSupport.colorLike(token)) {
                color = token;
            } else if (readingSize) {
                size.add(token);
            } else if (POSITION.contains(lower) || UiCssShorthandSupport.lengthLike(lower)) {
                position.add(token);
            } else {
                position.add(token);
            }
        }

        if (!color.isBlank()) out.add(new UiCssDeclaration("background-color", color));
        if (!image.isBlank()) out.add(new UiCssDeclaration("background-image", image));
        if (!position.isEmpty()) out.add(new UiCssDeclaration("background-position", String.join(" ", position)));
        if (!size.isEmpty()) out.add(new UiCssDeclaration("background-size", String.join(" ", size)));
        if (!repeat.isBlank()) out.add(new UiCssDeclaration("background-repeat", repeat));
        return out;
    }
}
