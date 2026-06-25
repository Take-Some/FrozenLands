package dev.takesome.helix.ui.render.awt;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.icons.EngineIconLoader;
import dev.takesome.helix.icons.selection.IcoImageSelector;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiBoxShadow;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.render.UiRenderContext;
import dev.takesome.helix.ui.icons.UiIcon;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/** Java2D/AWT adapter for retained engine.ui nodes used by desktop crash windows. */
public final class AwtUiRenderContext implements UiRenderContext {
    private static final Font UI_FONT = loadResourceFont("/helix/ui/fonts/FSElliotPro.ttf", "Segoe UI", Font.PLAIN);
    private static final Font UI_BOLD = loadResourceFont("/helix/ui/fonts/FSElliotPro-Bold.ttf", "Segoe UI", Font.BOLD);
    private static final Font UI_TITLE = loadResourceFont("/helix/ui/fonts/FSElliotPro-Heavy.ttf", "Segoe UI", Font.BOLD);
    private static final Font UI_MONO = loadResourceFont("/helix/ui/fonts/FSElliotPro.ttf", "Segoe UI", Font.PLAIN);
    private static final Font FA_SOLID_FONT = loadResourceFont("/dev/takesome/helix/ui/icons/fontawesome/fa-solid-900.ttf", "Dialog", Font.PLAIN);
    private static final int ARC = 18;

    private final EngineIconLoader iconLoader = new EngineIconLoader();
    private final Map<String, BufferedImage> images = new ConcurrentHashMap<>();
    private final Deque<Shape> clipStack = new ArrayDeque<>();

    private Graphics2D graphics;
    private int viewportHeight;

    public void begin(Graphics2D graphics, int viewportHeight) {
        this.graphics = graphics;
        this.viewportHeight = viewportHeight;
        applyUiRenderingHints(graphics);
    }

    public static void applyUiRenderingHints(Graphics2D graphics) {
        if (graphics == null) {
            return;
        }
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, 140);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    public void end() {
        graphics = null;
        viewportHeight = 0;
    }

    @Override
    public void fill(UiRect rect, UiColor color) {
        if (graphics == null || rect == null || color == null || rect.w <= 0f || rect.h <= 0f || color.a <= 0f) {
            return;
        }
        java.awt.Composite oldComposite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.SrcOver.derive(color.a));
        graphics.setColor(toAwt(color));
        graphics.fill(new RoundRectangle2D.Float(
                Math.round(rect.x),
                Math.round(y(rect)),
                Math.round(rect.w),
                Math.round(rect.h),
                ARC,
                ARC
        ));
        graphics.setComposite(oldComposite);
    }

    @Override
    public void stroke(UiRect rect, UiColor color, float width) {
        if (graphics == null || rect == null || color == null || rect.w <= 0f || rect.h <= 0f || color.a <= 0f || width <= 0f) {
            return;
        }
        java.awt.Composite oldComposite = graphics.getComposite();
        java.awt.Stroke oldStroke = graphics.getStroke();
        graphics.setComposite(AlphaComposite.SrcOver.derive(color.a));
        graphics.setStroke(new BasicStroke(Math.max(1f, width)));
        graphics.setColor(toAwt(color));
        float inset = Math.max(0.5f, width * 0.5f);
        graphics.draw(new RoundRectangle2D.Float(
                rect.x + inset,
                y(rect) + inset,
                Math.max(0f, rect.w - inset * 2f),
                Math.max(0f, rect.h - inset * 2f),
                ARC,
                ARC
        ));
        graphics.setStroke(oldStroke);
        graphics.setComposite(oldComposite);
    }


    @Override
    public void boxShadow(UiRect rect, UiBoxShadow shadow) {
        if (graphics == null || rect == null || shadow == null || !shadow.visible() || rect.w <= 0f || rect.h <= 0f) return;
        java.awt.Composite oldComposite = graphics.getComposite();
        Shape oldClip = graphics.getClip();
        float blur = Math.max(0f, shadow.blurRadius());
        float spread = shadow.spreadRadius();
        int steps = Math.max(1, Math.min(18, Math.round(blur / 2f) + 1));
        if (shadow.inset()) graphics.clip(new java.awt.Rectangle(Math.round(rect.x), Math.round(y(rect)), Math.round(rect.w), Math.round(rect.h)));
        for (int i = steps; i >= 1; i--) {
            float t = i / (float) steps;
            float grow = spread + blur * t;
            float alpha = shadow.color().a * (1f - t * 0.72f) / steps;
            if (steps == 1) alpha = shadow.color().a;
            graphics.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
            graphics.setColor(toAwt(shadow.color()));
            graphics.fill(new RoundRectangle2D.Float(
                    rect.x + shadow.offsetX() - grow,
                    y(rect) - shadow.offsetY() - grow,
                    Math.max(0f, rect.w + grow * 2f),
                    Math.max(0f, rect.h + grow * 2f),
                    ARC + grow,
                    ARC + grow
            ));
        }
        graphics.setClip(oldClip);
        graphics.setComposite(oldComposite);
    }

    @Override
    public void text(String text, UiRect rect, float scale, UiColor color, TextAlign align) {
        drawText(text, rect, scale, color, align, null, 0f, true);
    }

    @Override
    public void text(String text, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) {
        drawText(text, rect, scale, color, align, fontId, 0f, true);
    }

    @Override
    public void buttonText(String text, UiRect rect, float scale, UiColor color, TextAlign align) {
        drawSingleLineCentered(text, rect, scale, color, align, "bold");
    }


    @Override
    public boolean pushClip(UiRect rect) {
        if (graphics == null || rect == null || rect.w <= 0f || rect.h <= 0f) return false;
        clipStack.push(graphics.getClip());
        graphics.clip(new java.awt.Rectangle(Math.round(rect.x), Math.round(y(rect)), Math.round(rect.w), Math.round(rect.h)));
        return true;
    }

    @Override
    public void popClip() {
        if (graphics == null || clipStack.isEmpty()) return;
        graphics.setClip(clipStack.pop());
    }

    @Override
    public boolean drawElement(UiElementSkin element, UiRect rect, UiColor tint) {
        // AWT crash UI cannot resolve game/editor skin atlases. Returning false lets
        // retained nodes use their semantic fallback colors instead of white element tints.
        return false;
    }

    @Override
    public boolean supportsIcons() {
        return true;
    }

    @Override
    public boolean supportsImages() {
        return true;
    }

    @Override
    public boolean icon(UiIcon icon, UiRect rect, float scale, UiColor color, TextAlign align) {
        if (graphics == null || icon == null || rect == null || rect.w <= 0f || rect.h <= 0f) return false;
        UiColor safeColor = color == null ? UiColor.WHITE : color;
        java.awt.Composite oldComposite = graphics.getComposite();
        Font oldFont = graphics.getFont();
        graphics.setComposite(AlphaComposite.SrcOver.derive(safeColor.a));
        graphics.setColor(toAwt(safeColor));

        float size = Math.max(8f, Math.min(rect.w, rect.h) * Math.max(0.45f, scale));
        Font font = FA_SOLID_FONT.deriveFont(size);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);
        String glyph = icon.text();
        int x = Math.round(rect.x + (rect.w - metrics.stringWidth(glyph)) * 0.5f);
        int topY = Math.round(y(rect));
        int baseline = topY + Math.round((rect.h - metrics.getHeight()) * 0.5f + metrics.getAscent());
        graphics.drawString(glyph, x, baseline);

        graphics.setFont(oldFont);
        graphics.setComposite(oldComposite);
        return true;
    }

    @Override
    public boolean image(String source, UiRect rect, float opacity) {
        if (graphics == null || source == null || source.isBlank() || rect == null || rect.w <= 0f || rect.h <= 0f) return false;
        BufferedImage image = imageResource(source);
        if (image == null) return false;

        java.awt.Composite oldComposite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, opacity))));
        graphics.drawImage(
                image,
                Math.round(rect.x),
                Math.round(y(rect)),
                Math.round(rect.w),
                Math.round(rect.h),
                null
        );
        graphics.setComposite(oldComposite);
        return true;
    }

    public void drawDetails(String text, UiRect rect, float scrollY) {
        drawText(text, rect, 0.92f, new UiColor(0.10f, 0.14f, 0.22f, 1.0f), TextAlign.LEFT, "mono", scrollY, false);
    }

    public void drawScrollbar(UiRect rect, float scrollY, float contentHeight) {
        if (graphics == null || rect == null || contentHeight <= rect.h) return;
        float trackW = 6f;
        float trackX = rect.x + rect.w - trackW - 6f;
        UiRect track = new UiRect(trackX, rect.y + 8f, trackW, rect.h - 16f);
        fill(track, new UiColor(0.86f, 0.88f, 0.91f, 1.0f));

        float ratio = Math.max(0.12f, Math.min(1f, rect.h / contentHeight));
        float thumbH = Math.max(28f, track.h * ratio);
        float maxScroll = Math.max(1f, contentHeight - rect.h);
        float normalized = Math.max(0f, Math.min(1f, scrollY / maxScroll));
        float thumbY = track.y + (track.h - thumbH) * (1f - normalized);
        fill(new UiRect(track.x, thumbY, track.w, thumbH), new UiColor(0.00f, 0.72f, 0.76f, 0.95f));
    }

    public float measureDetailsHeight(String text) {
        Font font = UI_MONO.deriveFont(13.0f);
        FontMetrics metrics = graphics == null ? null : graphics.getFontMetrics(font);
        int lineHeight = metrics == null ? 18 : Math.max(18, Math.round(metrics.getHeight() * 1.12f));
        return Math.max(lineHeight, lines(text).size() * lineHeight + 18f);
    }

    private void drawSingleLineCentered(String value, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) {
        if (graphics == null || rect == null || value == null || value.isEmpty() || rect.w <= 0f || rect.h <= 0f) return;
        UiColor safeColor = color == null ? UiColor.WHITE : color;
        java.awt.Composite oldComposite = graphics.getComposite();
        Shape oldClip = graphics.getClip();
        Font oldFont = graphics.getFont();
        graphics.setComposite(AlphaComposite.SrcOver.derive(safeColor.a));
        graphics.setColor(toAwt(safeColor));

        Font font = font(fontId).deriveFont(sizeFor(fontId, scale));
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);

        int x = Math.round(rect.x);
        int topY = Math.round(y(rect));
        int w = Math.round(rect.w);
        int h = Math.round(rect.h);
        graphics.clip(new java.awt.Rectangle(x, topY, w, h));

        String line = value.replace('\n', ' ').replace('\r', ' ').trim();
        int lineX = x + 8;
        if (align == TextAlign.CENTER) lineX = x + Math.max(0, (w - metrics.stringWidth(line)) / 2);
        if (align == TextAlign.RIGHT) lineX = x + Math.max(8, w - metrics.stringWidth(line) - 8);
        int baseline = topY + Math.round((h - metrics.getHeight()) * 0.5f + metrics.getAscent());
        graphics.drawString(line, lineX, baseline);

        graphics.setClip(oldClip);
        graphics.setFont(oldFont);
        graphics.setComposite(oldComposite);
    }

    private void drawText(String value, UiRect rect, float scale, UiColor color, TextAlign align, String fontId, float scrollY, boolean wrap) {
        if (graphics == null || rect == null || value == null || value.isEmpty() || rect.w <= 0f || rect.h <= 0f) return;
        UiColor safeColor = color == null ? UiColor.WHITE : color;
        java.awt.Composite oldComposite = graphics.getComposite();
        Shape oldClip = graphics.getClip();
        Font oldFont = graphics.getFont();
        graphics.setComposite(AlphaComposite.SrcOver.derive(safeColor.a));
        graphics.setColor(toAwt(safeColor));

        Font font = font(fontId).deriveFont(sizeFor(fontId, scale));
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);

        int x = Math.round(rect.x);
        int topY = Math.round(y(rect));
        int w = Math.round(rect.w);
        int h = Math.round(rect.h);
        graphics.clip(new java.awt.Rectangle(x, topY, w, h));

        List<String> logicalLines = wrap ? wrappedLines(value, metrics, Math.max(16, w - 16)) : lines(value);
        int lineHeight = Math.max(12, Math.round(metrics.getHeight() * (fontId != null && fontId.toLowerCase(Locale.ROOT).contains("mono") ? 1.18f : 1.05f)));
        int baseline = topY + 4 + metrics.getAscent() - Math.round(scrollY);
        for (String line : logicalLines) {
            if (baseline > topY - lineHeight && baseline < topY + h + lineHeight) {
                int lineX = x + 8;
                if (align == TextAlign.CENTER) lineX = x + Math.max(0, (w - metrics.stringWidth(line)) / 2);
                if (align == TextAlign.RIGHT) lineX = x + Math.max(8, w - metrics.stringWidth(line) - 8);
                graphics.drawString(line, lineX, baseline);
            }
            baseline += lineHeight;
        }

        graphics.setClip(oldClip);
        graphics.setFont(oldFont);
        graphics.setComposite(oldComposite);
    }

    private List<String> wrappedLines(String value, FontMetrics metrics, int maxWidth) {
        List<String> out = new ArrayList<>();
        for (String sourceLine : lines(value)) {
            if (metrics.stringWidth(sourceLine) <= maxWidth) {
                out.add(sourceLine);
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : sourceLine.split(" ")) {
                String next = current.isEmpty() ? word : current + " " + word;
                if (metrics.stringWidth(next) <= maxWidth) {
                    current.setLength(0);
                    current.append(next);
                } else {
                    if (!current.isEmpty()) out.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                }
            }
            if (!current.isEmpty()) out.add(current.toString());
        }
        return out;
    }

    private List<String> lines(String value) {
        String[] split = value.replace("\r", "").split("\n", -1);
        List<String> out = new ArrayList<>(split.length);
        for (String line : split) out.add(emptyIfNull(line));
        return out;
    }

    private Font font(String fontId) {
        String id = emptyIfNull(fontId).toLowerCase(Locale.ROOT);
        if (id.contains("mono") || id.contains("code")) return UI_MONO;
        if (id.contains("title")) return UI_TITLE;
        if (id.contains("bold") || id.contains("heading")) return UI_BOLD;
        return UI_FONT;
    }

    private float sizeFor(String fontId, float scale) {
        String id = emptyIfNull(fontId).toLowerCase(Locale.ROOT);
        float base = id.contains("mono") || id.contains("code") ? 14f : 14f;
        if (id.contains("title")) base = 22f;
        if (id.contains("bold") || id.contains("heading")) base = 13f;
        return Math.max(9f, base * Math.max(0.55f, scale));
    }

    private static Font loadResourceFont(String resourcePath, String fallbackFamily, int fallbackStyle) {
        try (InputStream in = AwtUiRenderContext.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(fallbackStyle, 14f);
            }
        } catch (Exception ignored) {
        }
        return new Font(fallbackFamily, fallbackStyle, 14);
    }

    private BufferedImage imageResource(String source) {
        String key = normalizeImageSource(source);
        BufferedImage cached = images.get(key);
        if (cached != null) return cached;
        BufferedImage loaded = loadImage(key);
        if (loaded != null) images.put(key, loaded);
        return loaded;
    }

    private String normalizeImageSource(String source) {
        String value = trimToEmpty(source);
        if (value.startsWith("classpath:")) value = value.substring("classpath:".length()).trim();
        return value;
    }

    private BufferedImage loadImage(String source) {
        if (isIcoSource(source)) {
            BufferedImage icon = loadIconImage(source);
            if (icon != null) return icon;
        }

        try (InputStream in = AwtUiRenderContext.class.getResourceAsStream(classpathPath(source))) {
            if (in != null) return ImageIO.read(in);
        } catch (Exception ignored) {
        }
        try {
            Path path = Path.of(source);
            if (Files.isRegularFile(path)) return ImageIO.read(path.toFile());
        } catch (Exception ignored) {
        }
        return null;
    }

    private BufferedImage loadIconImage(String source) {
        try {
            BufferedImage[] icons = iconLoader.loadAppIcons(source, null, AwtUiRenderContext.class.getClassLoader());
            BufferedImage best = IcoImageSelector.getBestMatchingIcon(icons, 64, 64);
            if (best != null) return best;
            return IcoImageSelector.getBestIcon(icons);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isIcoSource(String source) {
        return source != null && source.toLowerCase(Locale.ROOT).endsWith(".ico");
    }

    private String classpathPath(String source) {
        if (source == null || source.isBlank()) return "/";
        return source.startsWith("/") ? source : "/" + source;
    }

    private Color toAwt(UiColor color) {
        return new Color(color.r, color.g, color.b);
    }

    private float y(UiRect rect) {
        return viewportHeight - rect.y - rect.h;
    }
}
