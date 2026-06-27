package org.takesome.frozenlands;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

import java.util.Locale;

final class StartupLoadingScreen {
    private static final String DEFAULT_FONT = "Interface/Fonts/Default.fnt";

    private static final ColorRGBA BACKGROUND = new ColorRGBA(0.025f, 0.035f, 0.052f, 1f);
    private static final ColorRGBA PANEL = new ColorRGBA(0.055f, 0.075f, 0.105f, 1f);
    private static final ColorRGBA TRACK = new ColorRGBA(0.12f, 0.145f, 0.18f, 1f);
    private static final ColorRGBA BAR = new ColorRGBA(0.22f, 0.62f, 0.95f, 1f);
    private static final ColorRGBA BAR_GLOW = new ColorRGBA(0.55f, 0.84f, 1f, 1f);
    private static final ColorRGBA TEXT = new ColorRGBA(0.86f, 0.91f, 0.98f, 1f);
    private static final ColorRGBA MUTED = new ColorRGBA(0.54f, 0.62f, 0.72f, 1f);
    private static final ColorRGBA ACCENT = new ColorRGBA(0.42f, 0.78f, 1f, 1f);

    private final AssetManager assetManager;
    private final Node guiNode;
    private final Node root = new Node("Startup Loading Screen");
    private final Geometry background;
    private final Geometry panel;
    private final Geometry progressTrack;
    private final Geometry progressFill;
    private final Geometry progressGlow;
    private final BitmapText overlineText;
    private final BitmapText titleText;
    private final BitmapText statusText;
    private final BitmapText percentText;

    private float progress;
    private int lastWidth = -1;
    private int lastHeight = -1;

    StartupLoadingScreen(AssetManager assetManager, Node guiNode) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        BitmapFont font = assetManager.loadFont(DEFAULT_FONT);

        this.background = rectangle("startup.background", BACKGROUND);
        this.panel = rectangle("startup.panel", PANEL);
        this.progressTrack = rectangle("startup.progress.track", TRACK);
        this.progressFill = rectangle("startup.progress.fill", BAR);
        this.progressGlow = rectangle("startup.progress.glow", BAR_GLOW);

        this.overlineText = text(font, "TAKE SOME() RUNTIME", 16f, ACCENT);
        this.titleText = text(font, "FrozenLands", 34f, TEXT);
        this.statusText = text(font, "Preparing runtime...", 17f, MUTED);
        this.percentText = text(font, "0%", 16f, TEXT);

        root.setCullHint(Spatial.CullHint.Always);
        root.attachChild(background);
        root.attachChild(panel);
        root.attachChild(progressTrack);
        root.attachChild(progressFill);
        root.attachChild(progressGlow);
        root.attachChild(overlineText);
        root.attachChild(titleText);
        root.attachChild(statusText);
        root.attachChild(percentText);
    }

    void show(int width, int height) {
        if (root.getParent() == null) {
            guiNode.attachChild(root);
        }
        root.setCullHint(Spatial.CullHint.Never);
        layout(width, height);
    }

    void update(String stage, float progress, int width, int height) {
        show(width, height);
        this.progress = clamp(progress);
        statusText.setText(stage == null || stage.isBlank() ? "Loading..." : stage);
        percentText.setText(String.format(Locale.ROOT, "%d%%", Math.round(this.progress * 100f)));
        layout(width, height);
    }

    void hide() {
        root.setCullHint(Spatial.CullHint.Always);
        root.removeFromParent();
    }

    private void layout(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        boolean sameSize = width == lastWidth && height == lastHeight;
        lastWidth = width;
        lastHeight = height;

        float screenWidth = width;
        float screenHeight = height;
        float panelWidth = Math.min(620f, screenWidth - 72f);
        float panelHeight = 230f;
        float panelX = (screenWidth - panelWidth) * 0.5f;
        float panelY = (screenHeight - panelHeight) * 0.5f;
        float padding = 32f;
        float barWidth = panelWidth - padding * 2f;
        float barHeight = 14f;
        float barX = panelX + padding;
        float barY = panelY + 54f;

        if (!sameSize) {
            resize(background, screenWidth, screenHeight);
            background.setLocalTranslation(0f, 0f, 0f);

            resize(panel, panelWidth, panelHeight);
            panel.setLocalTranslation(panelX, panelY, 1f);

            resize(progressTrack, barWidth, barHeight);
            progressTrack.setLocalTranslation(barX, barY, 2f);

            resize(progressFill, barWidth, barHeight);
            progressFill.setLocalTranslation(barX, barY, 3f);

            resize(progressGlow, 4f, barHeight + 8f);

            overlineText.setLocalTranslation(panelX + padding, panelY + panelHeight - 44f, 4f);
            titleText.setLocalTranslation(panelX + padding, panelY + panelHeight - 82f, 4f);
            statusText.setLocalTranslation(panelX + padding, panelY + 104f, 4f);
            percentText.setLocalTranslation(panelX + panelWidth - padding - 54f, panelY + 104f, 4f);
        }

        progressFill.setLocalScale(Math.max(0.001f, progress), 1f, 1f);
        progressGlow.setLocalTranslation(barX + Math.max(0f, barWidth * progress - 2f), barY - 4f, 4f);
    }

    private Geometry rectangle(String name, ColorRGBA color) {
        Geometry geometry = new Geometry(name, new Quad(1f, 1f));
        geometry.setMaterial(material(color));
        return geometry;
    }

    private void resize(Geometry geometry, float width, float height) {
        geometry.setMesh(new Quad(Math.max(1f, width), Math.max(1f, height)));
    }

    private Material material(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        material.getAdditionalRenderState().setDepthTest(false);
        material.getAdditionalRenderState().setDepthWrite(false);
        return material;
    }

    private BitmapText text(BitmapFont font, String value, float size, ColorRGBA color) {
        BitmapText text = new BitmapText(font);
        text.setText(value);
        text.setSize(size);
        text.setColor(color);
        return text;
    }

    private float clamp(float value) {
        if (Float.isNaN(value)) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, value));
    }
}
