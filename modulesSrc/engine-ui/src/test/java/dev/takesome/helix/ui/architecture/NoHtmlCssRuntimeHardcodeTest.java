package dev.takesome.helix.ui.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NoHtmlCssRuntimeHardcodeTest {
    @Test
    void coreRuntimeFilesMustNotUseSwitchDispatchForHtmlOrCssVocabulary() throws IOException {
        assertNoToken("src/main/java/dev/takesome/helix/ui/css/UiCssLayoutEngine.java", "switch");
        assertNoToken("src/main/java/dev/takesome/helix/ui/markup/internal/factory/UiDomRetainedNodeFactory.java", "switch");
        assertNoToken("src/main/java/dev/takesome/helix/ui/markup/internal/compile/UiDomStyleBridge.java", "STYLE_FALLBACK_ATTRIBUTES");
    }

    @Test
    void engineUiMarkupMustNotUseBrowserScriptHandlers() throws IOException {
        assertNoToken("src/main/java", "onclick");
        assertNoToken("src/main/resources", "onclick");
        assertNoToken("src/main/java", "document.querySelector");
        assertNoToken("src/main/java", "eval(");
    }

    private void assertNoToken(String moduleRelativePath, String token) throws IOException {
        Path path = Path.of(moduleRelativePath);
        if (!Files.exists(path)) path = Path.of("modulesSrc/engine-ui").resolve(moduleRelativePath);
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.walk(path)) {
                for (Path file : stream.filter(Files::isRegularFile).filter(this::isRelevantTextFile).toList()) {
                    assertFileDoesNotContain(file, token);
                }
            }
            return;
        }
        assertFileDoesNotContain(path, token);
    }

    private boolean isRelevantTextFile(Path path) {
        String value = path.toString();
        return value.endsWith(".java") || value.endsWith(".html") || value.endsWith(".css") || value.endsWith(".lua");
    }

    private void assertFileDoesNotContain(Path path, String token) throws IOException {
        String content = Files.readString(path);
        assertFalse(content.contains(token), "Forbidden runtime hardcode token `" + token + "` found in " + path);
    }
}
