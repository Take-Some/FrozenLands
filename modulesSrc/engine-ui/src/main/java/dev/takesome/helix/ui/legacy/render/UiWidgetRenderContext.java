package dev.takesome.helix.ui.legacy.render;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.model.UiRect;

/** Immutable render call context passed to primitive widget renderers. */
public final class UiWidgetRenderContext {
    private final UiBindingSource binding;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final UiRect panelRect;
    private final UiWidgetDefinition widget;
    private final String key;
    private final float uiTime;

    public UiWidgetRenderContext(
            UiBindingSource binding,
            SpriteBatch batch,
            BitmapFont font,
            UiRect panelRect,
            UiWidgetDefinition widget,
            String key,
            float uiTime
    ) {
        this.binding = binding;
        this.batch = batch;
        this.font = font;
        this.panelRect = panelRect;
        this.widget = widget;
        this.key = emptyIfNull(key);
        this.uiTime = uiTime;
    }

    public UiBindingSource binding() { return binding; }
    public SpriteBatch batch() { return batch; }
    public BitmapFont font() { return font; }
    public UiRect panelRect() { return panelRect; }
    public UiWidgetDefinition widget() { return widget; }
    public String key() { return key; }
    public float uiTime() { return uiTime; }
}
