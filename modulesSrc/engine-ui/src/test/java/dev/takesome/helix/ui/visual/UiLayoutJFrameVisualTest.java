package dev.takesome.helix.ui.visual;


import static dev.takesome.helix.validation.EngineValidator.hasPositiveFiniteSize;
import static dev.takesome.helix.validation.EngineValidator.lowerTrimToEmpty;
import dev.takesome.helix.ui.css.UiCssBox;
import dev.takesome.helix.ui.css.UiCssCascade;
import dev.takesome.helix.ui.css.UiCssLayoutEngine;
import dev.takesome.helix.ui.css.UiCssLayoutResult;
import dev.takesome.helix.ui.css.UiCssParser;
import dev.takesome.helix.ui.css.UiStylesheet;
import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.dom.UiDomNode;
import dev.takesome.helix.ui.dom.UiDomText;
import dev.takesome.helix.ui.dom.UiDomTraversal;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opens a real Swing JFrame and paints HELIX DOM/CSS layout inside it. */
class UiLayoutJFrameVisualTest {
    private static final Path OUT = Path.of("build/reports/engine-ui/visual");

    @Test
    void opensRealJFrameAndRendersLayoutTabs() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "JFrame visual test requires a non-headless desktop");
        Files.createDirectories(OUT);

        DemoSpec flex = flexSpec();
        DemoSpec document = documentSpec();
        JFrame frame = createFrame(flex, document);
        JTabbedPane tabs = (JTabbedPane) frame.getContentPane();
        long showMs = Long.getLong("helix.ui.visual.frame.ms", 2600L);

        try {
            SwingUtilities.invokeAndWait(() -> {
                frame.setVisible(true);
                frame.toFront();
                frame.requestFocus();
            });
            TimeUnit.MILLISECONDS.sleep(Math.max(500L, showMs / 2L));
            SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(0));
            capture(frame, OUT.resolve("jframe-flex-box-layout.png"));
            SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(1));
            TimeUnit.MILLISECONDS.sleep(Math.max(500L, showMs / 2L));
            capture(frame, OUT.resolve("jframe-document-flow-lists-text.png"));
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }

        writeIndex();
        assertTrue(Files.size(OUT.resolve("jframe-flex-box-layout.png")) > 1500);
        assertTrue(Files.size(OUT.resolve("jframe-document-flow-lists-text.png")) > 1500);
    }

    private JFrame createFrame(DemoSpec flex, DemoSpec document) throws InvocationTargetException, InterruptedException {
        final JFrame[] out = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("HELIX engine-ui — real JFrame layout render");
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("FLEX box", new VisualDomPanel(flex));
            tabs.addTab("h1 h2 p br ul ol", new VisualDomPanel(document));
            frame.setContentPane(tabs);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(960, 760);
            frame.setLocationRelativeTo(null);
            out[0] = frame;
        });
        return out[0];
    }

    private void capture(JFrame frame, Path output) throws IOException, InvocationTargetException, InterruptedException {
        final BufferedImage[] image = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage rendered = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = rendered.createGraphics();
            try {
                frame.paint(g);
            } finally {
                g.dispose();
            }
            image[0] = rendered;
        });
        ImageIO.write(image[0], "png", output.toFile());
    }

    private DemoSpec flexSpec() {
        return new DemoSpec("""
                <body id="visual-flex">
                  <section class="demo-title">Real JFrame FLEX render</section>
                  <section class="flex-demo">
                    <div class="card fixed">fixed 140px</div>
                    <div class="card grow">flex-grow: 1</div>
                    <div class="card fixed">fixed 140px</div>
                  </section>
                  <section class="notes">JFrame paints DOM + CSS cascade + UiCssLayoutEngine output directly.</section>
                </body>
                """, """
                body { width: 920px; height: 700px; margin: 0; padding: 24px; background-color: #f8fafc; color: #0f172a; }
                .demo-title { height: 38px; margin-bottom: 18px; font-size: 26px; font-weight: bold; color: #0f172a; }
                .flex-demo { display: flex; flex-direction: row; width: 872px; height: 170px; padding: 18px; gap: 12px; justify-content: space-between; align-items: center; overflow: hidden; background-color: #eef6f8; border: 1px solid #8bd9e4; border-radius: 12px; }
                .card { height: 68px; padding: 12px; background-color: #ffffff; border: 1px solid #94a3b8; border-radius: 10px; color: #0f172a; text-align: center; }
                .fixed { width: 140px; }
                .grow { flex-grow: 1; flex-shrink: 1; flex-basis: 120px; min-width: 150px; max-width: 360px; background-color: #dffafe; border-color: #00a9bb; }
                .notes { height: 34px; margin-top: 20px; padding: 8px; background-color: #ffffff; border: 1px solid #cbd5e1; color: #475569; }
                """);
    }

    private DemoSpec documentSpec() {
        return new DemoSpec("""
                <body id="visual-document">
                  <h1>HELIX JFrame document flow</h1>
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
                """, """
                body { width: 920px; height: 700px; margin: 0; padding: 28px; background-color: #ffffff; color: #111827; }
                h1 { height: 44px; margin: 0 0 8px 0; font-size: 32px; font-weight: bold; color: #0f172a; }
                h2 { height: 34px; margin: 0 0 18px 0; font-size: 22px; font-weight: bold; color: #00a9bb; }
                p { height: 82px; margin: 0 0 18px 0; padding: 12px; background-color: #f8fafc; border: 1px solid #cbd5e1; border-radius: 10px; line-height: 24px; }
                br { height: 18px; }
                .lists { display: flex; flex-direction: row; gap: 24px; width: 864px; height: 170px; padding: 16px; background-color: #f1f5f9; border: 1px solid #cbd5e1; border-radius: 12px; }
                ul { width: 390px; height: 132px; margin: 0; padding-left: 34px; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; }
                ol { width: 390px; height: 132px; margin: 0; padding-left: 34px; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; }
                li { height: 30px; margin: 0; padding: 4px; font-size: 15px; color: #334155; }
                """);
    }

    private void writeIndex() throws IOException {
        Files.writeString(OUT.resolve("jframe-index.html"), """
                <!doctype html><html><head><meta charset="utf-8"><title>HELIX JFrame visual tests</title></head>
                <body style="font-family:sans-serif;background:#f8fafc;color:#0f172a">
                  <h1>HELIX real JFrame visual tests</h1>
                  <p>Generated by UiLayoutJFrameVisualTest. The test opens a real Swing JFrame, paints the layout, captures both tabs, and closes automatically.</p>
                  <h2>FLEX box tab</h2><img src="jframe-flex-box-layout.png" style="max-width:100%;border:1px solid #cbd5e1">
                  <h2>Document flow tab</h2><img src="jframe-document-flow-lists-text.png" style="max-width:100%;border:1px solid #cbd5e1">
                </body></html>
                """);
    }

    private record DemoSpec(String markup, String css) { }

    private static final class VisualDomPanel extends JPanel {
        private final DemoSpec spec;

        private VisualDomPanel(DemoSpec spec) {
            this.spec = spec;
            setPreferredSize(new Dimension(940, 700));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                UiDomDocument document = UiDomDocument.parse(spec.markup());
                UiStylesheet stylesheet = new UiCssParser().parse(spec.css());
                new UiCssCascade().apply(document, stylesheet);
                UiCssLayoutResult layout = new UiCssLayoutEngine().layout(document, getWidth(), getHeight());
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                for (UiDomElement element : UiDomTraversal.depthFirstElements(document.root())) drawElement(g, layout, element, getHeight());
                g.setFont(new Font("Monospaced", Font.PLAIN, 11));
                g.setColor(new Color(100, 116, 139));
                g.drawString("Real JFrame render: DOM + CSS cascade + UiCssLayoutEngine", 18, getHeight() - 18);
            } finally {
                g.dispose();
            }
        }

        private void drawElement(Graphics2D g, UiCssLayoutResult layout, UiDomElement element, int viewportH) {
            UiCssBox box = layout.box(element).orElse(null);
            if (box == null || !hasPositiveFiniteSize(box.width(), box.height()) || "none".equalsIgnoreCase(element.style("display"))) return;
            Shape oldClip = g.getClip();
            if (clips(element)) g.clip(rect(box, viewportH));
            Color bg = color(element.style("background-color"), null);
            if (bg != null && bg.getAlpha() > 0) {
                g.setColor(bg);
                g.fill(roundRect(box, viewportH, radius(element)));
            }
            Color border = color(element.style("border-color"), null);
            float borderW = length(element.style("border-width"), 0f);
            if (border != null && borderW > 0f) {
                g.setColor(border);
                g.setStroke(new BasicStroke(Math.max(1f, borderW)));
                g.draw(roundRect(box, viewportH, radius(element)));
            }
            drawText(g, element, box, viewportH);
            if (clips(element)) g.setClip(oldClip);
        }

        private void drawText(Graphics2D g, UiDomElement element, UiCssBox box, int viewportH) {
            List<String> lines = textLines(element);
            if (lines.isEmpty()) return;
            float fontSize = length(element.style("font-size"), tagFontSize(element.tagName()));
            boolean bold = element.style("font-weight").toLowerCase(Locale.ROOT).contains("bold") || element.tagName().matches("h[1-6]");
            Font font = new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, Math.max(9, Math.round(fontSize)));
            g.setFont(font);
            FontMetrics metrics = g.getFontMetrics(font);
            g.setColor(color(element.style("color"), new Color(17, 24, 39)));
            float lineHeight = length(element.style("line-height"), Math.max(fontSize + 4f, metrics.getHeight()));
            int x = Math.round(box.x() + length(element.style("padding-left"), 0f) + ("li".equals(element.tagName()) ? 20f : 8f));
            int baseline = Math.round(y(box, viewportH) + length(element.style("padding-top"), 0f) + metrics.getAscent() + 3f);
            if ("li".equals(element.tagName())) drawMarker(g, element, box, viewportH, baseline);
            for (String line : lines) {
                if (!line.isBlank()) g.drawString(line, x, baseline);
                baseline += Math.round(lineHeight);
            }
        }

        private void drawMarker(Graphics2D g, UiDomElement li, UiCssBox box, int viewportH, int baseline) {
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

        private boolean clips(UiDomElement element) {
            String overflow = first(element.style("overflow"), element.style("overflow-x"), element.style("overflow-y"));
            return "hidden".equals(overflow) || "auto".equals(overflow) || "scroll".equals(overflow);
        }

        private String first(String... values) {
            for (String value : values) if (value != null && !value.isBlank()) return value.trim().toLowerCase(Locale.ROOT);
            return "";
        }

        private RoundRectangle2D.Float roundRect(UiCssBox box, int viewportH, float radius) {
            return new RoundRectangle2D.Float(box.x(), y(box, viewportH), box.width(), box.height(), radius, radius);
        }

        private java.awt.Rectangle rect(UiCssBox box, int viewportH) {
            return new java.awt.Rectangle(Math.round(box.x()), Math.round(y(box, viewportH)), Math.round(box.width()), Math.round(box.height()));
        }

        private float y(UiCssBox box, int viewportH) {
            return viewportH - box.y() - box.height();
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
            try { return Float.parseFloat(value); } catch (RuntimeException ignored) { return fallback; }
        }

        private Color color(String raw, Color fallback) {
            String value = lowerTrimToEmpty(raw, Locale.ROOT);
            if (value.isBlank()) return fallback;
            if ("transparent".equals(value)) return new Color(0, 0, 0, 0);
            if ("white".equals(value)) return Color.WHITE;
            if ("black".equals(value)) return Color.BLACK;
            if (value.startsWith("#")) return hex(value, fallback);
            return fallback;
        }

        private Color hex(String value, Color fallback) {
            try {
                String hex = value.substring(1);
                if (hex.length() == 3) return new Color(Integer.parseInt(hex.substring(0,1)+hex.substring(0,1),16), Integer.parseInt(hex.substring(1,2)+hex.substring(1,2),16), Integer.parseInt(hex.substring(2,3)+hex.substring(2,3),16));
                if (hex.length() == 6) return new Color(Integer.parseInt(hex.substring(0,2),16), Integer.parseInt(hex.substring(2,4),16), Integer.parseInt(hex.substring(4,6),16));
            } catch (RuntimeException ignored) { }
            return fallback;
        }
    }
}
