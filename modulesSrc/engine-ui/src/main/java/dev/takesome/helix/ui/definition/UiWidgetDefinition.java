package dev.takesome.helix.ui.definition;

import dev.takesome.helix.ui.animation.UiTextAnimationDefinition;

/** Generic widget described by JSON. No game-specific fields here. */
public final class UiWidgetDefinition {
    public String id;
    /** Script-owned component/type id. Java must not attach gameplay/UI semantics to it. */
    public String type;
    /** Engine drawing primitive selected by data/script. Backward-compatible fallback: type. */
    public String primitive;
    public String visible;

    public float x;
    public float y;
    public float w;
    public float h;

    public String text;
    /** Optional i18n key for JSON-driven UI documents. Localized pattern is bound after translation. */
    public String i18nKey;
    /** Alias for i18nKey accepted by data descriptors. */
    public String textKey;
    public String binding;

    /** Canonical flat animation id. Prefer textAnimation for parameters. */
    public String animation;

    /** Legacy alias accepted for concise JSON. */
    public String effect;

    /** Canonical text animation block. */
    public UiTextAnimationDefinition textAnimation;

    /** Legacy alias for textAnimation. */
    public UiTextAnimationDefinition textEffect;

    /** Canonical CSS-like font family/face name. Prefer this over legacy font. */
    public String fontFamily;
    /** Legacy alias for fontFamily kept for old documents. */
    public String font;
    public String align;
    public String progress;
    public String material;
    public String base;
    public String fill;
    public String icon;
    public int frame;

    /** Canonical CSS-like font size in UI pixels. Prefer this over legacy scale. */
    public float fontSize;
    /** Legacy text scale retained for old documents. */
    public float scale = 1f;
    public float iconSize = 18f;
    public float gap = 6f;

    /** Flat shorthand for text animation. Prefer textAnimation for complex definitions. */
    public float animationDelay;
    public float animationDuration;
    public float animationSpeed;
    public float effectDelay;
    public float effectDuration;
    public float effectSpeed;
    public float slideX;
    public float slideY;

    /** Optional primitive layout hints for bar-like drawing. Values are in UI pixels. */
    public float labelHeight;
    public float labelGap = -1f;
    public float trackHeight;
    public float[] trackInsets;

    public float[] color;
    public float[] fillColor;

    /** Runtime-only arrangement written by the document layout pass. */
    transient boolean layoutResolved;
    transient float layoutX;
    transient float layoutY;
    transient float layoutW;
    transient float layoutH;

    public String effectiveFontFamily() {
        return fontFamily != null && !fontFamily.isBlank() ? fontFamily : font;
    }

    public boolean hasFontSize() {
        return Float.isFinite(fontSize) && fontSize > 0f;
    }
}

