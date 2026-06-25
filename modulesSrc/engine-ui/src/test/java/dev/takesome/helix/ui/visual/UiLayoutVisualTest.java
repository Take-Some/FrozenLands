package dev.takesome.helix.ui.visual;


import static dev.takesome.helix.validation.EngineValidator.hasPositiveFiniteSize;
import static dev.takesome.helix.validation.EngineValidator.lowerTrimToEmpty;
import dev.takesome.helix.ui.model.UiBoxShadow;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.css.UiCssBox;
import dev.takesome.helix.ui.css.UiCssCascade;
import dev.takesome.helix.ui.css.UiCssLayoutEngine;
import dev.takesome.helix.ui.css.UiCssLayoutResult;
import dev.takesome.helix.ui.css.UiCssParser;
import dev.takesome.helix.ui.css.UiStylesheet;
import dev.takesome.helix.ui.css.runtime.UiCssBoxShadowParser;
import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.dom.UiDomNode;
import dev.takesome.helix.ui.dom.UiDomText;
import dev.takesome.helix.ui.dom.UiDomTraversal;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Human-inspectable visual layout fixtures.
 *
 * <p>These tests do not compare golden pixels. They generate PNG reports that show
 * how HELIX DOM + CSS cascade + layout resolve in practice.</p>
 */
class UiLayoutVisualTest {
    private static final int W = 900;
    private static final int H = 640;
    private static final Path OUT = Path.of("build/reports/engine-ui/visual");

    @Test
    void rendersFlexBoxLayoutVisualReport() throws IOException {
        String markup = """
                <body id="visual-flex">
                  <section class="demo-title">FLEX box runtime layout</section>
                  <section class="flex-demo">
                    <div class="card fixed">fixed 140px</div>
                    <div class="card grow">flex-grow: 1</div>
                    <div class="card fixed">fixed 140px</div>
                  </section>
                  <section class="notes">
                    justify-content: space-between | align-items: center | gap: 12px | box-shadow | overflow hidden
                  </section>
                </body>
                """;
        String css = """
                body { width: 900px; height: 640px; margin: 0; padding: 24px; background-color: #f8fafc; color: #0f172a; }
                .demo-title { height: 38px; margin-bottom: 18px; font-size: 26px; font-weight: bold; color: #0f172a; }
                .flex-demo { display: flex; flex-direction: row; width: 852px; height: 170px; padding: 18px; gap: 12px; justify-content: space-between; align-items: center; overflow: hidden; background-color: #eef6f8; border: 1px solid #8bd9e4; border-radius: 12px; box-shadow: 0 8px 24px rgba(15,23,42,0.16); }
                .card { height: 68px; padding: 12px; background-color: #ffffff; border: 1px solid #94a3b8; border-radius: 10px; box-shadow: 0 4px 16px rgba(0,0,0,0.10); color: #0f172a; text-align: center; }
                .fixed { width: 140px; }
                .grow { flex-grow: 1; flex-shrink: 1; flex-basis: 120px; min-width: 150px; max-width: 360px; background-color: #dffafe; border-color: #00a9bb; }
                .notes { height: 34px; margin-top: 20px; padding: 8px; background-color: #ffffff; border: 1px solid #cbd5e1; color: #475569; }
                """;
        Path image = render("flex-box-layout", markup, css);
        assertImage(image);
    }

    @Test
    void rendersTextListsAndLineBreakVisualReport() throws IOException {
        String markup = """
                <body id="visual-document">
                  <h1>HELIX document flow</h1>
                  <h2>h1, h2, p, br, ul, ol</h2>
                  <p>Paragraph line one is laid out inside a block.<br />Paragraph line two is produced by a br element.</p>
                  <section class="lists">
                    <ul>
                      <li>unordered item alpha</li>
                      <li>unordered item beta</li>
                      <li>unordered item gamma</li>
                    </ul>
                    <ol>
                      <li>ordered item one</li>
                      <li>ordered item two</li>
                      <li>ordered item three</li>
                    </ol>
                  </section>
                </body>
                """;
        String css = """
                body { width: 900px; height: 640px; margin: 0; padding: 28px; background-color: #ffffff; color: #111827; }
                h1 { height: 44px; margin: 0 0 8px 0; font-size: 32px; font-weight: bold; color: #0f172a; }
                h2 { height: 34px; margin: 0 0 18px 0; font-size: 22px; font-weight: bold; color: #00a9bb; }
                p { height: 82px; margin: 0 0 18px 0; padding: 12px; background-color: #f8fafc; border: 1px solid #cbd5e1; border-radius: 10px; line-height: 24px; }
                br { height: 18px; }
                .lists { display: flex; flex-direction: row; gap: 24px; width: 844px; height: 170px; padding: 16px; background-color: #f1f5f9; border: 1px solid #cbd5e1; border-radius: 12px; }
                ul { width: 380px; height: 132px; margin: 0; padding-left: 34px; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; }
                ol { width: 380px; height: 132px; margin: 0; padding-left: 34px; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; }
                li { height: 30px; margin: 0; padding: 4px; font-size: 15px; color: #334155; }
                """;
        Path image = render("document-flow-lists-text", markup, css);
        assertImage(image);
    }

    private Path render(String name, String markup, String css) throws IOException {
        Files.createDirectories(OUT);
        UiDomDocument document = UiDomDocument.parse(markup);
        UiStylesheet stylesheet = new UiCssParser().parse(css);
        new UiCssCascade().apply(document, stylesheet);
        UiCssLayoutResult layout = new UiCssLayoutEngine().layout(document, W, H);

        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, W, H);
            for (UiDomElement element : UiDomTraversal.depthFirstElements(document.root())) drawElement(g, layout, element);
            drawReportFooter(g, name);
        } finally {
            g.dispose();
        }

        Path imagePath = OUT.resolve(name + ".png");
        ImageIO.write(image, "png", imagePath.toFile());
        writeIndex();
        return imagePath;
    }

    private void drawElement(Graphics2D g, UiCssLayoutResult layout, UiDomElement element) {
        UiCssBox box = layout.box(element).orElse(null);
        if (box == null || !hasPositiveFiniteSize(box.width(), box.height()) || "none".equalsIgnoreCase(element.style("display"))) return;

        Shape oldClip = g.getClip();
        boolean clip = clips(element);
        if (clip) g.clip(rect(box));

        drawBoxShadow(g, element, box);
        Color background = color(element.style("background-color"), null);
        if (background != null && background.getAlpha() > 0) {
            g.setColor(background);
            g.fill(roundRect(box, radius(element)));
        }
        drawBorder(g, element, box);
        drawText(g, element, box);

        if (clip) g.setClip(oldClip);
    }

    private void drawBoxShadow(Graphics2D g, UiDomElement element, UiCssBox box) {
        String raw = element.style("box-shadow");
        if (raw == null || raw.isBlank() || "none".equalsIgnoreCase(raw.trim())) return;
        for (UiBoxShadow shadow : new UiCssBoxShadowParser().parse(raw)) {
            if (shadow == null || !shadow.visible() || shadow.inset()) continue;
            int steps = Math.max(1, Math.min(16, Math.round(shadow.blurRadius() / 2f) + 1));
            Color c = awt(shadow.color());
            for (int i = steps; i >= 1; i--) {
                float t = i / (float) steps;
                float grow = shadow.spreadRadius() + shadow.blurRadius() * t;
                float alpha = shadow.color().a * (1f - t * 0.72f) / steps;
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, Math.round(alpha * 255f)))));
                g.fill(new RoundRectangle2D.Float(
                        box.x() + shadow.offsetX() - grow,
                        y(box) - shadow.offsetY() - grow,
                        Math.max(0f, box.width() + grow * 2f),
                        Math.max(0f, box.height() + grow * 2f),
                        radius(element) + grow,
                        radius(element) + grow
                ));
            }
        }
    }

    private void drawBorder(Graphics2D g, UiDomElement element, UiCssBox box) {
        Color border = color(element.style("border-color"), null);
        float borderWidth = length(element.style("border-width"), 0f);
        if (border == null || border.getAlpha() == 0 || borderWidth <= 0f) return;
        g.setColor(border);
        g.setStroke(new BasicStroke(Math.max(1f, borderWidth)));
        g.draw(roundRect(box, radius(element)));
    }

    private void drawText(Graphics2D g, UiDomElement element, UiCssBox box) {
        List<String> lines = textLines(element);
        if (lines.isEmpty()) return;
        float fontSize = length(element.style("font-size"), tagFontSize(element.tagName()));
        boolean bold = element.style("font-weight").toLowerCase(Locale.ROOT).contains("bold") || element.tagName().matches("h[1-6]");
        Font font = new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, Math.max(9, Math.round(fontSize)));
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics(font);
        Color text = color(element.style("color"), new Color(17, 24, 39));
        g.setColor(text);

        float paddingLeft = length(element.style("padding-left"), 0f);
        float paddingTop = length(element.style("padding-top"), 0f);
        float lineHeight = length(element.style("line-height"), Math.max(fontSize + 4f, metrics.getHeight()));
        int baseX = Math.round(box.x() + paddingLeft + ("li".equals(element.tagName()) ? 20f : 8f));
        int baseline = Math.round(y(box) + paddingTop + metrics.getAscent() + 3f);

        if ("li".equals(element.tagName())) drawListMarker(g, element, box, baseline);
        for (String line : lines) {
            if (!line.isBlank()) g.drawString(line, baseX, baseline);
            baseline += Math.round(lineHeight);
        }
    }

    private void drawListMarker(Graphics2D g, UiDomElement li, UiCssBox box, int baseline) {
        UiDomElement parent = li.parent();
        if (parent == null) return;
        String marker = "ul".equals(parent.tagName()) ? "•" : listIndex(li) + ".";
        g.drawString(marker, Math.round(box.x() + 4f), baseline);
    }

    private int listIndex(UiDomElement li) {
        UiDomElement parent = li.parent();
        if (parent == null) return 1;
        int index = 1;
        for (UiDomNode child : parent.children()) {
            if (child instanceof UiDomElement element && "li".equals(element.tagName())) {
                if (element == li) return index;
                index++;
            }
        }
        return index;
    }

    private List<String> textLines(UiDomElement element) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (UiDomNode child : element.children()) {
            if (child instanceof UiDomText text) current.append(text.text());
            if (child instanceof UiDomElement childElement && "br".equals(childElement.tagName())) {
                lines.add(current.toString().trim());
                current.setLength(0);
            }
        }
        String tail = current.toString().trim();
        if (!tail.isBlank()) lines.add(tail);
        return lines;
    }

    private void drawReportFooter(Graphics2D g, String name) {
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(100, 116, 139));
        g.drawString("HELIX engine-ui visual layout test: " + name + " | generated PNG, human-inspectable", 18, H - 18);
    }

    private void writeIndex() throws IOException {
        String html = """
                <!doctype html>
                <html><head><meta charset="utf-8"><title>HELIX engine-ui visual tests</title></head>
                <body style="font-family: sans-serif; background: #f8fafc; color: #0f172a;">
                  <h1>HELIX engine-ui visual layout tests</h1>
                  <p>Generated by UiLayoutVisualTest. These images are reports, not golden snapshots.</p>
                  <h2>FLEX box</h2><img src="flex-box-layout.png" style="max-width: 100%; border: 1px solid #cbd5e1;">
                  <h2>Document flow: h1, h2, p, br, ul, ol</h2><img src="document-flow-lists-text.png" style="max-width: 100%; border: 1px solid #cbd5e1;">
                </body></html>
                """;
        Files.writeString(OUT.resolve("index.html"), html);
    }

    private void assertImage(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "visual report image was not written: " + path);
        assertTrue(Files.size(path) > 1500, "visual report image is unexpectedly small: " + path);
    }

    private boolean clips(UiDomElement element) {
        String overflow = first(element.style("overflow"), element.style("overflow-x"), element.style("overflow-y"));
        return "hidden".equals(overflow) || "auto".equals(overflow) || "scroll".equals(overflow);
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim().toLowerCase(Locale.ROOT);
        return "";
    }

    private RoundRectangle2D.Float roundRect(UiCssBox box, float radius) {
        return new RoundRectangle2D.Float(box.x(), y(box), box.width(), box.height(), radius, radius);
    }

    private java.awt.Rectangle rect(UiCssBox box) {
        return new java.awt.Rectangle(Math.round(box.x()), Math.round(y(box)), Math.round(box.width()), Math.round(box.height()));
    }

    private float y(UiCssBox box) {
        return H - box.y() - box.height();
    }

    private float radius(UiDomElement element) {
        return Math.max(0f, length(element.style("border-radius"), 0f));
    }

    private float tagFontSize(String tag) {
        return switch (tag) {
            case "h1" -> 32f;
            case "h2" -> 24f;
            case "h3" -> 19f;
            default -> 16f;
        };
    }

    private float length(String raw, float fallback) {
        String value = lowerTrimToEmpty(raw, Locale.ROOT);
        if (value.isBlank() || "auto".equals(value) || "normal".equals(value)) return fallback;
        if (value.endsWith("px")) value = value.substring(0, value.length() - 2).trim();
        try {
            return Float.parseFloat(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private Color color(String raw, Color fallback) {
        String value = lowerTrimToEmpty(raw, Locale.ROOT);
        if (value.isBlank()) return fallback;
        if ("transparent".equals(value)) return new Color(0, 0, 0, 0);
        if ("white".equals(value)) return Color.WHITE;
        if ("black".equals(value)) return Color.BLACK;
        if (value.startsWith("#")) return hex(value, fallback);
        if (value.startsWith("rgb(")) return rgb(value, fallback, false);
        if (value.startsWith("rgba(")) return rgb(value, fallback, true);
        return fallback;
    }

    private Color hex(String value, Color fallback) {
        try {
            String hex = value.substring(1);
            if (hex.length() == 3) return new Color(
                    Integer.parseInt(hex.substring(0, 1) + hex.substring(0, 1), 16),
                    Integer.parseInt(hex.substring(1, 2) + hex.substring(1, 2), 16),
                    Integer.parseInt(hex.substring(2, 3) + hex.substring(2, 3), 16));
            if (hex.length() == 6) return new Color(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16));
            if (hex.length() == 8) return new Color(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16), Integer.parseInt(hex.substring(6, 8), 16));
        } catch (RuntimeException ignored) {
        }
        return fallback;
    }

    private Color rgb(String value, Color fallback, boolean alpha) {
        int open = value.indexOf('(');
        int close = value.lastIndexOf(')');
        if (open < 0 || close <= open) return fallback;
        String[] parts = value.substring(open + 1, close).split("\\s*,\\s*");
        if (parts.length < 3) return fallback;
        int r = Math.round(channel(parts[0]) * 255f);
        int g = Math.round(channel(parts[1]) * 255f);
        int b = Math.round(channel(parts[2]) * 255f);
        int a = alpha && parts.length > 3 ? Math.round(alpha(parts[3]) * 255f) : 255;
        return new Color(clamp255(r), clamp255(g), clamp255(b), clamp255(a));
    }

    private float channel(String raw) {
        String value = raw.trim();
        if (value.endsWith("%")) return clamp01(number(value.substring(0, value.length() - 1), 0f) / 100f);
        return clamp01(number(value, 0f) / 255f);
    }

    private float alpha(String raw) {
        String value = raw.trim();
        if (value.endsWith("%")) return clamp01(number(value.substring(0, value.length() - 1), 100f) / 100f);
        return clamp01(number(value, 1f));
    }

    private float number(String raw, float fallback) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private Color awt(UiColor color) {
        return new Color(color.r, color.g, color.b, color.a);
    }
}
