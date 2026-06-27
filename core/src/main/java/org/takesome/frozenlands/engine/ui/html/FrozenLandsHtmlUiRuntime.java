package org.takesome.frozenlands.engine.ui.html;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.takesome.htmldom.desktop.HtmlDomEventDispatcher;
import dev.takesome.htmldom.desktop.HtmlDomSwingPanel;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.scripting.lua.HtmlDomLuaRuntime;
import org.takesome.frozenlands.engine.EngineContext;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FrozenLandsHtmlUiRuntime {
    public static final String DEFAULT_UI_ROOT = "assets/ui";
    public static final String DEFAULT_MANIFEST = "ui.manifest.json";

    private final EngineContext context;
    private final Path uiRoot;
    private final FrozenLandsHtmlUiManifest manifest;
    private final HtmlDomSwingPanel panel;
    private final HtmlDomLuaRuntime luaRuntime;
    private final String html;
    private final String css;
    private final List<String> loadedScripts;

    private FrozenLandsHtmlUiRuntime(
            EngineContext context,
            Path uiRoot,
            FrozenLandsHtmlUiManifest manifest,
            HtmlDomSwingPanel panel,
            HtmlDomLuaRuntime luaRuntime,
            String html,
            String css,
            List<String> loadedScripts
    ) {
        this.context = context;
        this.uiRoot = uiRoot;
        this.manifest = manifest;
        this.panel = panel;
        this.luaRuntime = luaRuntime;
        this.html = html;
        this.css = css;
        this.loadedScripts = List.copyOf(loadedScripts);
    }

    public static FrozenLandsHtmlUiRuntime loadDefault(EngineContext context) throws IOException {
        Path uiRoot = resolveUiRoot();
        return load(context, uiRoot.resolve(DEFAULT_MANIFEST));
    }

    public static FrozenLandsHtmlUiRuntime load(EngineContext context, Path manifestPath) throws IOException {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(manifestPath, "manifestPath");

        Path normalizedManifest = manifestPath.toAbsolutePath().normalize();
        Path uiRoot = normalizedManifest.getParent();
        ObjectMapper objectMapper = new ObjectMapper();
        FrozenLandsHtmlUiManifest manifest = objectMapper.readValue(normalizedManifest.toFile(), FrozenLandsHtmlUiManifest.class);
        validateManifest(manifest, uiRoot);

        String html = readRelative(uiRoot, manifest.getEntry());
        String css = readStyles(uiRoot, manifest.getStyles());
        HtmlDomSwingPanel panel = new HtmlDomSwingPanel(html, css);
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        panel.setDoubleBuffered(false);
        panel.setFocusable(false);

        HtmlDomLuaRuntime luaRuntime = new HtmlDomLuaRuntime(panel.document());
        List<String> loadedScripts = executeScripts(uiRoot, manifest.getScripts(), luaRuntime);
        FrozenLandsHtmlUiRuntime runtime = new FrozenLandsHtmlUiRuntime(context, uiRoot, manifest, panel, luaRuntime, html, css, loadedScripts);
        runtime.installDefaultEventBridge();
        runtime.setScreen(manifest.getStartupScreen());
        return runtime;
    }

    private static Path resolveUiRoot() {
        String explicit = System.getProperty("frozenlands.uiRoot");
        if (explicit != null && !explicit.isBlank()) {
            return Path.of(explicit).toAbsolutePath().normalize();
        }

        String runtimeRoot = System.getProperty("frozenlands.runtimeRoot");
        if (runtimeRoot != null && !runtimeRoot.isBlank()) {
            Path candidate = Path.of(runtimeRoot).resolve(DEFAULT_UI_ROOT).toAbsolutePath().normalize();
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }

        return Path.of(DEFAULT_UI_ROOT).toAbsolutePath().normalize();
    }

    private static void validateManifest(FrozenLandsHtmlUiManifest manifest, Path uiRoot) throws IOException {
        if (manifest.getEntry() == null || manifest.getEntry().isBlank()) {
            throw new IOException("HTML UI manifest entry is blank");
        }
        requireFile(uiRoot, manifest.getEntry(), "entry");
        for (String style : manifest.getStyles()) {
            requireFile(uiRoot, style, "style");
        }
        for (String script : manifest.getScripts()) {
            requireFile(uiRoot, script, "script");
        }
        if (manifest.getBindings() != null && !manifest.getBindings().isBlank()) {
            requireFile(uiRoot, manifest.getBindings(), "bindings");
        }
    }

    private static void requireFile(Path uiRoot, String relative, String role) throws IOException {
        Path path = resolveInsideUiRoot(uiRoot, relative);
        if (!Files.isRegularFile(path)) {
            throw new IOException("HTML UI " + role + " file is missing: " + relative + " resolved=" + path);
        }
    }

    private static String readRelative(Path uiRoot, String relative) throws IOException {
        return Files.readString(resolveInsideUiRoot(uiRoot, relative), StandardCharsets.UTF_8);
    }

    private static String readStyles(Path uiRoot, List<String> styles) throws IOException {
        StringBuilder css = new StringBuilder(8192);
        for (String style : styles) {
            css.append("\n/* ").append(style).append(" */\n");
            css.append(readRelative(uiRoot, style)).append('\n');
        }
        return css.toString();
    }

    private static List<String> executeScripts(Path uiRoot, List<String> scripts, HtmlDomLuaRuntime luaRuntime) throws IOException {
        List<String> loaded = new ArrayList<>();
        for (String script : scripts) {
            String code = readRelative(uiRoot, script);
            luaRuntime.execute(code, script);
            loaded.add(script);
        }
        return loaded;
    }

    private static Path resolveInsideUiRoot(Path uiRoot, String relative) throws IOException {
        if (relative == null || relative.isBlank()) {
            throw new IOException("Blank HTML UI resource path");
        }
        String normalizedRelative = relative.replace('\\', '/');
        if (normalizedRelative.startsWith("/") || normalizedRelative.contains("../")) {
            throw new IOException("HTML UI resource escapes ui root: " + relative);
        }
        Path resolved = uiRoot.resolve(normalizedRelative).toAbsolutePath().normalize();
        if (!resolved.startsWith(uiRoot.toAbsolutePath().normalize())) {
            throw new IOException("HTML UI resource escapes ui root: " + relative);
        }
        return resolved;
    }

    private void installDefaultEventBridge() {
        try {
            panel.addEventListener("[data-action]", "click", this::handleClick);
        } catch (RuntimeException exception) {
            context.getLogger().warn("Html UI click bridge was not installed: {}", exception.toString());
        }
    }

    private void handleClick(HtmlDomEventDispatcher.DomEvent event) {
        UiDomElement target = event.currentTarget() == null ? event.target() : event.currentTarget();
        if (target == null) {
            return;
        }
        String action = target.data("action", "");
        String elementId = target.id() == null ? "" : target.id();
        if (action.isBlank()) {
            return;
        }

        boolean consumed = false;
        try {
            consumed = luaRuntime.call("onClick", action, elementId);
        } catch (RuntimeException exception) {
            context.getLogger().error("Html UI Lua onClick failed action={} elementId={} message={}", action, elementId, exception.toString());
        }
        if (consumed) {
            event.preventDefault();
        }
        try {
            luaRuntime.call("afterClick", action, elementId);
        } catch (RuntimeException exception) {
            context.getLogger().error("Html UI Lua afterClick failed action={} elementId={} message={}", action, elementId, exception.toString());
        }
        panel.invalidateLayout();
        context.getModuleRegistry().publishEvent("ui.html.action", java.util.Map.of(
                "action", action,
                "elementId", elementId,
                "consumed", consumed
        ));
    }

    public BufferedImage renderToImage(int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        panel.setSize(safeWidth, safeHeight);
        panel.doLayout();
        panel.ensureLayout();

        BufferedImage image = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, safeWidth, safeHeight);
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    public void setScreen(String screen) {
        String safeScreen = screen == null || screen.isBlank() ? "hud" : screen;
        document().getElementById("fl-root").ifPresent(root -> {
            root.setAttribute("data-screen", safeScreen);
            root.setAttribute("data-mode", modeForScreen(safeScreen));
        });
        document().getElementById("screen-hud").ifPresent(node -> node.setAttribute("aria-hidden", "hud".equals(safeScreen) ? "false" : "true"));
        document().getElementById("screen-pause").ifPresent(node -> node.setAttribute("aria-hidden", "pause".equals(safeScreen) ? "false" : "true"));
        document().getElementById("screen-main-menu").ifPresent(node -> node.setAttribute("aria-hidden", "main-menu".equals(safeScreen) ? "false" : "true"));
        panel.invalidateLayout();
        panel.repaintHost();
    }

    private String modeForScreen(String screen) {
        if ("main-menu".equals(screen)) {
            return "boot";
        }
        if ("pause".equals(screen)) {
            return "paused";
        }
        return "gameplay";
    }

    public String screen() {
        return document().getElementById("fl-root").map(root -> root.data("screen", "hud")).orElse("hud");
    }

    public void openDevTools() {
        panel.openDevTools();
    }

    public UiDomDocument document() {
        return panel.document();
    }

    public long documentVersion() {
        return panel.document().version();
    }

    public HtmlDomSwingPanel panel() {
        return panel;
    }

    public Path uiRoot() {
        return uiRoot;
    }

    public FrozenLandsHtmlUiManifest manifest() {
        return manifest;
    }

    public String html() {
        return html;
    }

    public String css() {
        return css;
    }

    public List<String> loadedScripts() {
        return loadedScripts;
    }

    public Optional<String> bindingPath() {
        return Optional.ofNullable(manifest.getBindings()).filter(value -> !value.isBlank());
    }
}
