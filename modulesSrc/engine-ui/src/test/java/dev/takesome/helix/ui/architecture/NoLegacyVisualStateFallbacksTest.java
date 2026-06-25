package dev.takesome.helix.ui.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NoLegacyVisualStateFallbacksTest {
    private static final List<String> FORBIDDEN_RUNTIME_TOKENS = List.of(
            "hover-color",
            "hoverColor",
            "pressed-color",
            "pressedColor",
            "before-content",
            "beforeContent",
            "after-content",
            "afterContent"
    );

    @Test
    void engineUiRuntimeMustNotReadLegacyVisualStateKnobs() throws IOException {
        Path root = Path.of("src/main/java");
        if (!Files.exists(root)) root = Path.of("modulesSrc/engine-ui/src/main/java");

        try (Stream<Path> stream = Files.walk(root)) {
            for (Path file : stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList()) {
                String content = Files.readString(file);
                for (String token : FORBIDDEN_RUNTIME_TOKENS) {
                    assertFalse(
                            content.contains(token),
                            "Forbidden legacy visual-state token `" + token + "` found in " + file
                    );
                }
            }
        }
    }
}
