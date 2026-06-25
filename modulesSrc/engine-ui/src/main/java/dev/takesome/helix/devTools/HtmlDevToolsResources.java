package dev.takesome.helix.devTools;

/**
 * Built-in HELIX HTML DevTools resource contract.
 *
 * <p>DevTools markup, styles and Lua scene bootstrap are engine-ui-owned built-ins.
 * They must not be loaded from a concrete game asset root.</p>
 */
public final class HtmlDevToolsResources {
    public static final String LUA_SCENE_MODULE = "engine-ui.lua.devtools_scene";
    public static final String LUA_SCENE_RESOURCE = "engine-ui/lua/devtools_scene.lua";
    public static final String MARKUP_RESOURCE = "engine-ui/devtools/ui/devtools.html";
    public static final String LIBRARY_MARKUP_RESOURCE = "engine-ui/devtools/ui/devtools.library.html";
    public static final String STYLESHEET_RESOURCE = "engine-ui/devtools/ui/devtools.css";

    public static final String PANEL_KIND = "html-css-viewer";
    private HtmlDevToolsResources() {
    }
}
