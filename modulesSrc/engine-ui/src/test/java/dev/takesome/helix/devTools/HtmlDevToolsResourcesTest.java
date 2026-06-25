package dev.takesome.helix.devTools;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class HtmlDevToolsResourcesTest {
    @Test
    void htmlDevToolsResourcesAreBuiltInEngineUiResources() {
        ClassLoader loader = HtmlDevToolsResources.class.getClassLoader();

        assertNotNull(loader.getResource(HtmlDevToolsResources.LUA_SCENE_RESOURCE));
        assertNotNull(loader.getResource(HtmlDevToolsResources.MARKUP_RESOURCE));
        assertNotNull(loader.getResource(HtmlDevToolsResources.STYLESHEET_RESOURCE));
    }

    @Test
    void htmlDevToolsResourcesAreNotOwnedByTopDownAssets() {
        Path root = workspaceRoot();

        assertFalse(Files.exists(root.resolve("assets/topdown/ui/devtools")));
        assertFalse(Files.exists(root.resolve("assets/topdown/devtools")));
    }

    private static Path workspaceRoot() {
        Path cursor = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && cursor != null; i++) {
            if (Files.exists(cursor.resolve("settings.gradle")) && Files.exists(cursor.resolve("modulesSrc"))) return cursor;
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Cannot resolve Java2DGame workspace root");
    }
}
