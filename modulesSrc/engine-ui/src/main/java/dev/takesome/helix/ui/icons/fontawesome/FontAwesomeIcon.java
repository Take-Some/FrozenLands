package dev.takesome.helix.ui.icons.fontawesome;

import dev.takesome.helix.ui.icons.UiIcon;

/** Common contract for bundled Font Awesome icon enum families. */
public interface FontAwesomeIcon extends UiIcon {
    String FAMILY_ID = "fontawesome";

    @Override
    default String familyId() {
        return FAMILY_ID;
    }

    @Override
    default String id() {
        return FAMILY_ID + ":" + styleId() + ":" + symbolicName();
    }
}
