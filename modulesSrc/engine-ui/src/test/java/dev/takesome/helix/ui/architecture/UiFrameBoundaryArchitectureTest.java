package dev.takesome.helix.ui.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class UiFrameBoundaryArchitectureTest {
    private static final List<String> AUTHORING_PACKAGES = List.of(
            "dev.takesome.helix.ui.markup",
            "dev.takesome.helix.ui.html",
            "dev.takesome.helix.ui.dom",
            "dev.takesome.helix.ui.css",
            "dev.takesome.helix.ui.binding",
            "lua"
    );

    @Test
    void frameAndBackendPackagesMustNotDependOnAuthoringFormats() throws IOException {
        assertNoAuthoringDependencies("src/main/java/dev/takesome/helix/ui/frame");
        assertNoAuthoringDependencies("src/main/java/dev/takesome/helix/ui/backend");
        assertNoAuthoringDependencies("src/main/java/dev/takesome/helix/ui/render");
    }

    @Test
    void retainedSceneMustKeepFrameBackendBehindExplicitRuntimeSwitch() throws IOException {
        Path path = sourcePath("src/main/java/dev/takesome/helix/ui/scene/NodeScene.java");
        String content = Files.readString(path);
        assertFalse(
                !content.contains("helix.ui.frameRender"),
                "NodeScene must keep UiFrame backend behind an explicit runtime switch until IR rendering is production-default."
        );
    }


    private void assertNoAuthoringDependencies(String moduleRelativeRoot) throws IOException {
        Path root = sourcePath(moduleRelativeRoot);
        if (!Files.exists(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path file : stream.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(file).toLowerCase();
                for (String token : AUTHORING_PACKAGES) {
                    assertFalse(
                            content.contains(token.toLowerCase()),
                            "Authoring dependency `" + token + "` found in runtime/backend file " + file
                    );
                }
            }
        }
    }

    private Path sourcePath(String moduleRelativePath) {
        Path path = Path.of(moduleRelativePath);
        if (!Files.exists(path)) path = Path.of("modulesSrc/engine-ui").resolve(moduleRelativePath);
        return path;
    }
}
