package dev.takesome.helix.ui.animation;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
/** Mutable render frame produced by the UI text animation pipeline. */
public final class UiTextAnimationFrame {
    public String text;
    public float alpha = 1f;
    public float offsetX;
    public float offsetY;
    public boolean visible = true;

    public UiTextAnimationFrame(String text) {
        this.text = emptyIfNull(text);
    }

    public static UiTextAnimationFrame hidden() {
        UiTextAnimationFrame frame = new UiTextAnimationFrame("");
        frame.visible = false;
        frame.alpha = 0f;
        return frame;
    }
}
