package dev.takesome.helix.devTools;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlDevToolsEngineUiRendererTest {
    private static final String FORBIDDEN_DESKTOP_UI_IMPORT = "import javax." + "swing";
    private static final String FORBIDDEN_AWT_IMPORT = "import java." + "awt";

    @Test
    void productionDevToolsMustNotUseSwingWidgetInspector() throws IOException {
        Path root = Path.of("src/main/java/dev/takesome/helix/devTools");

        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("JFrame"), file + " must not use JFrame");
                assertFalse(source.contains("JTree"), file + " must not use Swing DOM tree");
                assertFalse(source.contains("JTable"), file + " must not use Swing CSS tables");
                assertFalse(source.contains("JTabbedPane"), file + " must not use Swing tabs");
                assertFalse(source.contains("JTextArea"), file + " must not use Swing source editor");
            }
        }
    }

    @Test
    void floatingWindowIsOnlyNativeHostAndContentIsEngineUi() throws IOException {
        String window = Files.readString(Path.of("src/main/java/dev/takesome/helix/devTools/HtmlDevToolsWindow.java"));
        assertTrue(window.contains("JWindow"));
        assertTrue(window.contains("HtmlDevToolsInspectorNode"));
        assertTrue(window.contains("AwtUiRenderContext"));
        assertFalse(window.contains("new JTree"));
        assertFalse(window.contains("new JTable"));
        assertFalse(window.contains("new JTabbedPane"));
    }

    @Test
    void devToolsUsesEngineUiSystemFontsInsteadOfGameFontAliases() throws IOException {
        String inspector = Files.readString(Path.of("src/main/java/dev/takesome/helix/devTools/HtmlDevToolsInspectorNode.java"));
        String resolver = Files.readString(Path.of("src/main/java/dev/takesome/helix/ui/render/EngineUiSystemFonts.java"));
        String textDrawer = Files.readString(Path.of("src/main/java/dev/takesome/helix/ui/render/GdxUiTextDrawer.java"));

        assertTrue(inspector.contains("engine-ui-system-fs-elliot-pro"));
        assertTrue(resolver.contains("Engine-ui-owned system font resolver"));
        assertTrue(textDrawer.contains("EngineUiSystemFonts.resolve"));
        assertFalse(inspector.contains("Public Pixel"));
        assertFalse(inspector.contains("Cairopixel"));
    }

}
