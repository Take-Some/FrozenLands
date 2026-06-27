package org.takesome.frozenlands.engine.player.menu;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

import java.util.Locale;

final class PlayerMenuView {
    private static final float PANEL_WIDTH = 520f;
    private static final float PANEL_HEIGHT = 320f;
    private static final float SLIDER_WIDTH = 360f;
    private static final float SLIDER_HEIGHT = 10f;
    private static final float SLIDER_HIT_HEIGHT = 42f;
    private static final float KNOB_WIDTH = 14f;
    private static final float KNOB_HEIGHT = 30f;
    private static final float RESUME_WIDTH = 180f;
    private static final float RESUME_HEIGHT = 44f;

    private final AssetManager assetManager;
    private final Camera camera;
    private final Node guiNode;
    private final Node root = new Node("Player Pause Menu");

    private BitmapFont font;
    private BitmapText titleText;
    private BitmapText hintText;
    private BitmapText lookScaleText;
    private BitmapText resumeText;
    private Geometry backdrop;
    private Geometry panel;
    private Geometry sliderTrack;
    private Geometry sliderFill;
    private Geometry sliderKnob;
    private Geometry resumeButton;

    private float panelX;
    private float panelY;
    private float sliderX;
    private float sliderY;
    private float resumeX;
    private float resumeY;
    private boolean initialized;

    PlayerMenuView(AssetManager assetManager, Camera camera, Node guiNode) {
        this.assetManager = assetManager;
        this.camera = camera;
        this.guiNode = guiNode;
    }

    void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        backdrop = quad("menu.backdrop", 1f, 1f, new ColorRGBA(0f, 0f, 0f, 0.58f));
        panel = quad("menu.panel", PANEL_WIDTH, PANEL_HEIGHT, new ColorRGBA(0.04f, 0.06f, 0.08f, 0.92f));
        sliderTrack = quad("menu.look.track", SLIDER_WIDTH, SLIDER_HEIGHT, new ColorRGBA(0.22f, 0.26f, 0.31f, 1f));
        sliderFill = quad("menu.look.fill", 1f, 1f, new ColorRGBA(0.58f, 0.74f, 0.94f, 1f));
        sliderKnob = quad("menu.look.knob", KNOB_WIDTH, KNOB_HEIGHT, new ColorRGBA(0.84f, 0.91f, 1f, 1f));
        resumeButton = quad("menu.resume", RESUME_WIDTH, RESUME_HEIGHT, new ColorRGBA(0.13f, 0.18f, 0.24f, 0.96f));

        titleText = text("FROZEN LANDS", 30f, ColorRGBA.White);
        hintText = text("ESC - back to game", 15f, new ColorRGBA(0.72f, 0.78f, 0.86f, 1f));
        lookScaleText = text("Mouse look sensitivity", 18f, ColorRGBA.White);
        resumeText = text("RESUME", 18f, ColorRGBA.White);

        root.attachChild(backdrop);
        root.attachChild(panel);
        root.attachChild(titleText);
        root.attachChild(hintText);
        root.attachChild(lookScaleText);
        root.attachChild(sliderTrack);
        root.attachChild(sliderFill);
        root.attachChild(sliderKnob);
        root.attachChild(resumeButton);
        root.attachChild(resumeText);
        root.setQueueBucket(RenderQueue.Bucket.Gui);
        guiNode.attachChild(root);
        layout();
    }

    void setVisible(boolean visible) {
        root.setCullHint(visible ? Spatial.CullHint.Never : Spatial.CullHint.Always);
    }

    void dispose() {
        root.removeFromParent();
    }

    void setLookScale(float value, float min, float max) {
        layout();
        float safeMin = Math.min(min, max);
        float safeMax = Math.max(min, max);
        float clamped = FastMath.clamp(value, safeMin, safeMax);
        float ratio = safeMax <= safeMin ? 0f : (clamped - safeMin) / (safeMax - safeMin);
        float fillWidth = Math.max(1f, SLIDER_WIDTH * ratio);
        sliderFill.setLocalTranslation(sliderX, sliderY, 13f);
        sliderFill.setLocalScale(fillWidth, SLIDER_HEIGHT, 1f);
        sliderKnob.setLocalTranslation(sliderX + fillWidth - KNOB_WIDTH * 0.5f, sliderY - (KNOB_HEIGHT - SLIDER_HEIGHT) * 0.5f, 14f);
        lookScaleText.setText(String.format(Locale.ROOT, "Mouse look sensitivity: %.2f", clamped));
    }

    boolean lookScaleSliderContains(float x, float y) {
        return x >= sliderX && x <= sliderX + SLIDER_WIDTH && y >= sliderY - SLIDER_HIT_HEIGHT * 0.5f && y <= sliderY + SLIDER_HIT_HEIGHT * 0.5f;
    }

    boolean resumeContains(float x, float y) {
        return contains(x, y, resumeX, resumeY, RESUME_WIDTH, RESUME_HEIGHT);
    }

    float lookScaleForMouseX(float mouseX, float min, float max) {
        float ratio = FastMath.clamp((mouseX - sliderX) / SLIDER_WIDTH, 0f, 1f);
        return min + (max - min) * ratio;
    }

    private void layout() {
        float width = camera.getWidth();
        float height = camera.getHeight();
        panelX = (width - PANEL_WIDTH) * 0.5f;
        panelY = (height - PANEL_HEIGHT) * 0.5f;
        sliderX = panelX + 80f;
        sliderY = panelY + 145f;
        resumeX = panelX + (PANEL_WIDTH - RESUME_WIDTH) * 0.5f;
        resumeY = panelY + 46f;

        backdrop.setLocalTranslation(0f, 0f, 0f);
        backdrop.setLocalScale(width, height, 1f);
        panel.setLocalTranslation(panelX, panelY, 10f);
        titleText.setLocalTranslation(panelX + 34f, panelY + PANEL_HEIGHT - 42f, 20f);
        hintText.setLocalTranslation(panelX + 36f, panelY + PANEL_HEIGHT - 70f, 20f);
        lookScaleText.setLocalTranslation(sliderX, sliderY + 42f, 20f);
        sliderTrack.setLocalTranslation(sliderX, sliderY, 12f);
        resumeButton.setLocalTranslation(resumeX, resumeY, 12f);
        resumeText.setLocalTranslation(resumeX + 54f, resumeY + 29f, 20f);
    }

    private boolean contains(float x, float y, float left, float bottom, float width, float height) {
        return x >= left && x <= left + width && y >= bottom && y <= bottom + height;
    }

    private Geometry quad(String name, float width, float height, ColorRGBA color) {
        Geometry geometry = new Geometry(name, new Quad(width, height));
        geometry.setMaterial(material(color));
        geometry.setQueueBucket(RenderQueue.Bucket.Gui);
        return geometry;
    }

    private BitmapText text(String value, float size, ColorRGBA color) {
        BitmapText text = new BitmapText(font);
        text.setText(value);
        text.setSize(size);
        text.setColor(color);
        text.setQueueBucket(RenderQueue.Bucket.Gui);
        return text;
    }

    private Material material(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return material;
    }
}
