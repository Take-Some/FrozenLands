package dev.takesome.helix.ui.render;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiBoxShadow;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;

/**
 * Backend-neutral retained UI drawing contract.
 *
 * <p>Nodes submit semantic primitive UI draws here. Concrete adapters can route
 * those draws to LibGDX SpriteBatch/ShapeRenderer today and Aurelia/Vulkan draw
 * packets later without leaking backend details into the node tree.</p>
 */
public interface UiRenderContext {
    void fill(UiRect rect, UiColor color);

    default void stroke(UiRect rect, UiColor color, float width) {
    }

    default void boxShadow(UiRect rect, UiBoxShadow shadow) {
        if (rect == null || shadow == null || !shadow.visible() || shadow.inset()) return;
        float spread = shadow.spreadRadius();
        fill(new UiRect(
                rect.x + shadow.offsetX() - spread,
                rect.y + shadow.offsetY() - spread,
                Math.max(0f, rect.w + spread * 2f),
                Math.max(0f, rect.h + spread * 2f)
        ), shadow.color());
    }

    void text(String text, UiRect rect, float scale, UiColor color, TextAlign align);

    default void text(String text, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) {
        text(text, rect, scale, color, align);
    }

    default void buttonText(String text, UiRect rect, float scale, UiColor color, TextAlign align) {
        text(text, rect, scale, color, align);
    }

    default void buttonText(String text, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) {
        buttonText(text, rect, scale, color, align);
    }

    default boolean supportsIcons() {
        return false;
    }

    default boolean supportsElements() {
        return false;
    }

    default boolean supportsImages() {
        return false;
    }

    default boolean icon(UiIcon icon, UiRect rect, float scale, UiColor color, TextAlign align) {
        return false;
    }

    default boolean image(String source, UiRect rect, float opacity) {
        return false;
    }

    default boolean drawElement(UiElementSkin element, UiRect rect) {
        return false;
    }

    default boolean drawElement(UiElementSkin element, UiRect rect, UiColor tint) {
        return drawElement(element, rect);
    }

    default UiRect elementContentBounds(UiElementSkin element, UiRect rect) {
        return rect;
    }

    default boolean pushOpacity(float opacity) {
        return false;
    }

    default void popOpacity() {
    }

    default boolean pushClip(UiRect rect) {
        return false;
    }

    default void popClip() {
    }

    default boolean drawTracers() {
        return false;
    }

    default void traceNode(UiRect bounds, UiRect contentBounds, int depth, String label) {
    }
}
