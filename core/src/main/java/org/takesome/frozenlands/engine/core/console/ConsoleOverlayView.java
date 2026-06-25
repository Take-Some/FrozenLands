package org.takesome.frozenlands.engine.core.console;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

import java.util.List;

final class ConsoleOverlayView {
    private static final float HEIGHT_RATIO = 0.42f;
    private static final float PADDING = 16f;

    private final Node root = new Node("CoreConsoleOverlay");
    private final AssetManager assetManager;
    private final Camera camera;
    private final Node guiNode;

    private BitmapText outputText;
    private BitmapText inputText;
    private BitmapText suggestionsText;
    private boolean visible;

    ConsoleOverlayView(AssetManager assetManager, Camera camera, Node guiNode) {
        this.assetManager = assetManager;
        this.camera = camera;
        this.guiNode = guiNode;
    }

    void initialize() {
        buildScene();
        root.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(root);
    }

    void dispose() {
        root.removeFromParent();
    }

    void setVisible(boolean visible) {
        this.visible = visible;
        root.setCullHint(visible ? Node.CullHint.Never : Node.CullHint.Always);
    }

    boolean isVisible() {
        return visible;
    }

    void update(String input, List<String> outputLines, String suggestions, boolean cursorVisible) {
        if (outputText == null) {
            return;
        }
        outputText.setText(String.join("\n", outputLines));
        inputText.setText("> " + input + (cursorVisible ? "_" : " "));
        suggestionsText.setText(suggestions == null ? "" : suggestions);
    }

    private void buildScene() {
        root.detachAllChildren();
        float width = camera.getWidth();
        float height = camera.getHeight();
        float consoleHeight = height * HEIGHT_RATIO;
        float baseY = height - consoleHeight;

        root.attachChild(background(width, consoleHeight, baseY));
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        float textSize = Math.max(14f, font.getCharSet().getRenderedSize());

        outputText = text(font, textSize);
        outputText.setLocalTranslation(PADDING, height - PADDING, 1f);
        root.attachChild(outputText);

        inputText = text(font, textSize);
        inputText.setLocalTranslation(PADDING, baseY + 42f, 1f);
        root.attachChild(inputText);

        suggestionsText = text(font, textSize * 0.86f);
        suggestionsText.setLocalTranslation(PADDING, baseY + 18f, 1f);
        root.attachChild(suggestionsText);
    }

    private Geometry background(float width, float height, float y) {
        Geometry geometry = new Geometry("CoreConsoleBackground", new Quad(width, height));
        geometry.setLocalTranslation(0f, y, 0f);
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0.78f));
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geometry.setMaterial(material);
        return geometry;
    }

    private BitmapText text(BitmapFont font, float size) {
        BitmapText text = new BitmapText(font);
        text.setSize(size);
        text.setColor(ColorRGBA.White);
        return text;
    }
}
