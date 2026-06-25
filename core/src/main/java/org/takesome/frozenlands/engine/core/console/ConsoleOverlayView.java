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
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ConsoleOverlayView {
    private static final float HEIGHT_RATIO = 0.34f;
    private static final float MIN_HEIGHT = 248f;
    private static final float PADDING = 14f;
    private static final float TOOLBAR_HEIGHT = 34f;
    private static final float INPUT_HEIGHT = 38f;
    private static final float HINT_HEIGHT = 20f;
    private static final float BUTTON_WIDTH = 88f;
    private static final float BUTTON_HEIGHT = 24f;
    private static final float BUTTON_GAP = 8f;
    private static final int VISIBLE_OUTPUT_LINES = 8;
    private static final int MAX_COMPLETION_ROWS = 8;
    private static final int MAX_RENDERED_LINE_CHARS = 180;

    private static final ColorRGBA BACKGROUND = new ColorRGBA(0.015f, 0.025f, 0.045f, 0.84f);
    private static final ColorRGBA TOOLBAR = new ColorRGBA(0.025f, 0.055f, 0.090f, 0.94f);
    private static final ColorRGBA INPUT_BACKGROUND = new ColorRGBA(0.035f, 0.075f, 0.105f, 0.98f);
    private static final ColorRGBA COMPLETION_BACKGROUND = new ColorRGBA(0.018f, 0.040f, 0.065f, 0.98f);
    private static final ColorRGBA COMPLETION_HIGHLIGHT = new ColorRGBA(0.080f, 0.220f, 0.310f, 0.92f);
    private static final ColorRGBA SCROLL_TRACK = new ColorRGBA(0.060f, 0.100f, 0.130f, 0.70f);
    private static final ColorRGBA SCROLL_THUMB = new ColorRGBA(0.280f, 0.650f, 0.780f, 0.88f);
    private static final ColorRGBA BUTTON = new ColorRGBA(0.08f, 0.18f, 0.26f, 0.95f);
    private static final ColorRGBA BUTTON_ALT = new ColorRGBA(0.09f, 0.24f, 0.33f, 0.95f);
    private static final ColorRGBA TEXT = new ColorRGBA(0.84f, 0.94f, 1f, 1f);
    private static final ColorRGBA MUTED = new ColorRGBA(0.52f, 0.68f, 0.76f, 1f);
    private static final ColorRGBA PROMPT = new ColorRGBA(0.42f, 0.95f, 1f, 1f);
    private static final ColorRGBA SUCCESS = new ColorRGBA(0.56f, 1f, 0.68f, 1f);
    private static final ColorRGBA ERROR = new ColorRGBA(1f, 0.36f, 0.34f, 1f);
    private static final ColorRGBA WARNING = new ColorRGBA(1f, 0.74f, 0.34f, 1f);
    private static final ColorRGBA SYSTEM = new ColorRGBA(0.50f, 0.76f, 1f, 1f);

    private final Node root = new Node("CoreConsoleOverlay");
    private final AssetManager assetManager;
    private final Camera camera;
    private final Node guiNode;
    private final List<BitmapText> outputTexts = new ArrayList<>();
    private final List<BitmapText> completionTexts = new ArrayList<>();

    private BitmapText titleText;
    private BitmapText inputText;
    private BitmapText suggestionsText;
    private BitmapText clearButtonText;
    private BitmapText pasteButtonText;
    private Geometry completionBackground;
    private Geometry completionHighlight;
    private Geometry outputScrollbarTrack;
    private Geometry outputScrollbarThumb;
    private Rect clearButton;
    private Rect pasteButton;
    private float completionFirstBaseline;
    private float completionLineHeight;
    private float outputScrollbarY;
    private float outputScrollbarHeight;
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

    int visibleOutputLineCount() {
        return Math.max(1, outputTexts.size());
    }

    boolean hitClearButton(int x, int y) {
        return hit(clearButton, x, y);
    }

    boolean hitPasteButton(int x, int y) {
        return hit(pasteButton, x, y);
    }

    void update(
            String input,
            List<String> outputLines,
            String suggestions,
            boolean cursorVisible,
            List<String> completionLines,
            int selectedCompletionIndex,
            int outputScrollOffset
    ) {
        if (inputText == null) {
            return;
        }
        updateOutput(outputLines, outputScrollOffset);
        inputText.setText("> " + compact(input == null ? "" : input, MAX_RENDERED_LINE_CHARS) + (cursorVisible ? "_" : " "));
        inputText.setColor(PROMPT);
        suggestionsText.setText(suggestions == null ? "" : suggestions);
        suggestionsText.setColor(MUTED);
        updateCompletions(completionLines, selectedCompletionIndex);
    }

    private void updateOutput(List<String> outputLines, int outputScrollOffset) {
        List<String> visibleLines = visibleLines(outputLines, outputTexts.size(), outputScrollOffset);
        for (int index = 0; index < outputTexts.size(); index++) {
            BitmapText line = outputTexts.get(index);
            if (index < visibleLines.size()) {
                String value = compact(visibleLines.get(index), MAX_RENDERED_LINE_CHARS);
                line.setText(value);
                line.setColor(colorForLine(value));
            } else {
                line.setText("");
            }
        }
        updateOutputScrollbar(outputLines == null ? 0 : outputLines.size(), outputScrollOffset);
    }

    private void updateCompletions(List<String> completionLines, int selectedCompletionIndex) {
        boolean hasCompletions = completionLines != null && !completionLines.isEmpty();
        completionBackground.setCullHint(hasCompletions ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
        completionHighlight.setCullHint(hasCompletions && selectedCompletionIndex >= 0 ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);

        for (int index = 0; index < completionTexts.size(); index++) {
            BitmapText line = completionTexts.get(index);
            if (hasCompletions && index < completionLines.size()) {
                boolean selected = index == selectedCompletionIndex;
                line.setText((selected ? "› " : "  ") + compact(completionLines.get(index), MAX_RENDERED_LINE_CHARS));
                line.setColor(selected ? PROMPT : TEXT);
            } else {
                line.setText("");
            }
        }

        if (hasCompletions && selectedCompletionIndex >= 0) {
            float y = completionFirstBaseline - completionLineHeight * selectedCompletionIndex - completionLineHeight + 4f;
            completionHighlight.setLocalTranslation(PADDING, y, 2.4f);
        }
    }

    private void buildScene() {
        root.detachAllChildren();
        outputTexts.clear();
        completionTexts.clear();

        float width = camera.getWidth();
        float height = camera.getHeight();
        float consoleHeight = Math.max(MIN_HEIGHT, height * HEIGHT_RATIO);
        float baseY = height - consoleHeight;

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        float textSize = Math.max(14f, font.getCharSet().getRenderedSize());
        float lineHeight = textSize + 4f;
        completionLineHeight = Math.max(17f, textSize * 0.95f + 3f);

        float inputPanelY = baseY + PADDING + HINT_HEIGHT;
        float inputBaseline = inputPanelY + 25f;
        float hintBaseline = baseY + PADDING;
        float outputTop = height - TOOLBAR_HEIGHT - 12f;
        float outputBottom = inputPanelY + INPUT_HEIGHT + 12f;

        root.attachChild(panel("CoreConsoleBackground", 0f, baseY, width, consoleHeight, BACKGROUND));
        root.attachChild(panel("CoreConsoleToolbar", 0f, height - TOOLBAR_HEIGHT, width, TOOLBAR_HEIGHT, TOOLBAR));
        root.attachChild(panel("CoreConsoleInputBackground", PADDING, inputPanelY, width - PADDING * 2f, INPUT_HEIGHT, INPUT_BACKGROUND));

        titleText = text(font, textSize * 0.90f, SYSTEM);
        titleText.setText("FrozenLands Console  |  Tab list  Up/Down select  Enter fill/execute  Esc close");
        titleText.setLocalTranslation(PADDING, height - 12f, 2f);
        root.attachChild(titleText);

        float buttonY = height - TOOLBAR_HEIGHT + 5f;
        pasteButton = new Rect(width - PADDING - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        clearButton = new Rect(pasteButton.x - BUTTON_GAP - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        root.attachChild(button("CoreConsoleClearButton", clearButton, BUTTON));
        root.attachChild(button("CoreConsolePasteButton", pasteButton, BUTTON_ALT));

        clearButtonText = text(font, textSize * 0.78f, TEXT);
        clearButtonText.setText("Clear");
        clearButtonText.setLocalTranslation(clearButton.x + 22f, clearButton.y + 17f, 3f);
        root.attachChild(clearButtonText);

        pasteButtonText = text(font, textSize * 0.78f, TEXT);
        pasteButtonText.setText("Paste");
        pasteButtonText.setLocalTranslation(pasteButton.x + 22f, pasteButton.y + 17f, 3f);
        root.attachChild(pasteButtonText);

        int rows = Math.min(VISIBLE_OUTPUT_LINES, Math.max(1, (int) ((outputTop - outputBottom) / lineHeight) + 1));
        for (int index = 0; index < rows; index++) {
            BitmapText line = text(font, textSize, TEXT);
            line.setLocalTranslation(PADDING, outputTop - lineHeight * index, 2f);
            outputTexts.add(line);
            root.attachChild(line);
        }

        outputScrollbarY = outputBottom - lineHeight + 4f;
        outputScrollbarHeight = outputTop - outputScrollbarY + 4f;
        float scrollbarX = width - PADDING - 7f;
        outputScrollbarTrack = panel("CoreConsoleOutputScrollbarTrack", scrollbarX, outputScrollbarY, 4f, outputScrollbarHeight, SCROLL_TRACK);
        outputScrollbarTrack.setCullHint(Spatial.CullHint.Always);
        root.attachChild(outputScrollbarTrack);
        outputScrollbarThumb = panel("CoreConsoleOutputScrollbarThumb", scrollbarX - 1f, outputScrollbarY, 6f, 32f, SCROLL_THUMB);
        outputScrollbarThumb.setCullHint(Spatial.CullHint.Always);
        root.attachChild(outputScrollbarThumb);

        inputText = text(font, textSize, PROMPT);
        inputText.setLocalTranslation(PADDING + 8f, inputBaseline, 3.2f);
        root.attachChild(inputText);

        suggestionsText = text(font, textSize * 0.78f, MUTED);
        suggestionsText.setLocalTranslation(PADDING, hintBaseline, 3.2f);
        root.attachChild(suggestionsText);

        float completionY = inputPanelY + INPUT_HEIGHT + 8f;
        float completionHeight = completionLineHeight * MAX_COMPLETION_ROWS + 10f;
        completionFirstBaseline = completionY + completionHeight - 10f;
        completionBackground = panel("CoreConsoleCompletionBackground", PADDING, completionY, width - PADDING * 2f, completionHeight, COMPLETION_BACKGROUND);
        completionBackground.setCullHint(Spatial.CullHint.Always);
        root.attachChild(completionBackground);

        completionHighlight = panel("CoreConsoleCompletionHighlight", PADDING, completionFirstBaseline - completionLineHeight + 4f, width - PADDING * 2f, completionLineHeight, COMPLETION_HIGHLIGHT);
        completionHighlight.setCullHint(Spatial.CullHint.Always);
        root.attachChild(completionHighlight);

        for (int index = 0; index < MAX_COMPLETION_ROWS; index++) {
            BitmapText line = text(font, textSize * 0.82f, TEXT);
            line.setLocalTranslation(PADDING + 8f, completionFirstBaseline - completionLineHeight * index, 3f);
            completionTexts.add(line);
            root.attachChild(line);
        }
    }

    private boolean hit(Rect rect, int x, int y) {
        if (rect == null) {
            return false;
        }
        if (rect.contains(x, y)) {
            return true;
        }
        int flippedY = camera.getHeight() - y;
        return rect.contains(x, flippedY);
    }

    private List<String> visibleLines(List<String> lines, int max, int outputScrollOffset) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        int safeMax = Math.max(1, max);
        int maxOffset = Math.max(0, lines.size() - safeMax);
        int offset = Math.max(0, Math.min(outputScrollOffset, maxOffset));
        int to = Math.max(0, lines.size() - offset);
        int from = Math.max(0, to - safeMax);
        return lines.subList(from, to);
    }

    private void updateOutputScrollbar(int totalLines, int outputScrollOffset) {
        if (outputScrollbarTrack == null || outputScrollbarThumb == null) {
            return;
        }
        int visibleRows = visibleOutputLineCount();
        if (totalLines <= visibleRows || outputScrollbarHeight <= 0f) {
            outputScrollbarTrack.setCullHint(Spatial.CullHint.Always);
            outputScrollbarThumb.setCullHint(Spatial.CullHint.Always);
            return;
        }
        int maxOffset = Math.max(1, totalLines - visibleRows);
        int offset = Math.max(0, Math.min(outputScrollOffset, maxOffset));
        float thumbHeight = Math.max(24f, outputScrollbarHeight * ((float) visibleRows / (float) totalLines));
        float travel = Math.max(0f, outputScrollbarHeight - thumbHeight);
        float y = outputScrollbarY + travel * ((float) offset / (float) maxOffset);
        outputScrollbarTrack.setCullHint(Spatial.CullHint.Inherit);
        outputScrollbarThumb.setCullHint(Spatial.CullHint.Inherit);
        outputScrollbarThumb.setLocalTranslation(outputScrollbarThumb.getLocalTranslation().x, y, 2.5f);
        outputScrollbarThumb.setLocalScale(1f, thumbHeight / 32f, 1f);
    }

    private String compact(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private ColorRGBA colorForLine(String line) {
        if (line == null || line.isBlank()) {
            return MUTED;
        }
        String normalized = line.toLowerCase(Locale.ROOT);
        if (line.startsWith(">")) {
            return PROMPT;
        }
        if (normalized.contains("ok=false") || normalized.contains("error") || normalized.contains("exception")) {
            return ERROR;
        }
        if (normalized.contains("warn") || normalized.contains("no completion")) {
            return WARNING;
        }
        if (normalized.contains("ok=true") || normalized.contains("success")) {
            return SUCCESS;
        }
        if (normalized.contains("frozenlands") || normalized.contains("console")) {
            return SYSTEM;
        }
        return TEXT;
    }

    private Geometry panel(String name, float x, float y, float width, float height, ColorRGBA color) {
        Geometry geometry = new Geometry(name, new Quad(width, height));
        geometry.setLocalTranslation(x, y, 0f);
        geometry.setMaterial(material(color));
        return geometry;
    }

    private Geometry button(String name, Rect rect, ColorRGBA color) {
        return panel(name, rect.x, rect.y, rect.w, rect.h, color);
    }

    private Material material(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return material;
    }

    private BitmapText text(BitmapFont font, float size, ColorRGBA color) {
        BitmapText text = new BitmapText(font);
        text.setSize(size);
        text.setColor(color);
        return text;
    }

    private record Rect(float x, float y, float w, float h) {
        boolean contains(float px, float py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }
}
