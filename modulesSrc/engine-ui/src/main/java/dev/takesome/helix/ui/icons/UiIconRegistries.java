package dev.takesome.helix.ui.icons;

import dev.takesome.helix.ui.icons.fontawesome.FontAwesomeBundle;
import dev.takesome.helix.ui.icons.fontawesome.FontAwesomeStyle;
import dev.takesome.helix.ui.icons.registry.IconRegistry;

/**
 * Built-in icon registry factories.
 */
public final class UiIconRegistries {
    private UiIconRegistries() {
    }

    public static IconRegistry standard() {
        return fontAwesomeBundled();
    }

    public static IconRegistry fontAwesomeBundled() {
        return FontAwesomeBundle.registry();
    }

    public static IconRegistry fontAwesomeSolid() {
        return FontAwesomeStyle.SOLID.registry();
    }

    public static IconRegistry fontAwesomeRegular() {
        return FontAwesomeStyle.REGULAR.registry();
    }

    public static IconRegistry fontAwesomeBrands() {
        return FontAwesomeStyle.BRANDS.registry();
    }
}
