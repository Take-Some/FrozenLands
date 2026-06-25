package dev.takesome.helix.ui.architecture;

import dev.takesome.helix.ui.css.UiCssParser;
import dev.takesome.helix.ui.markup.UiMarkupParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiAuthoringResourceConformanceTest {
    @Test
    void authoredUiCssResourcesParseWithCurrentHelixCssSubset() throws IOException {
        Path root = workspaceRoot();
        UiCssParser parser = new UiCssParser();
        ArrayList<String> failures = new ArrayList<>();

        for (Path file : authoredResources(root, List.of(".css", ".styles"))) {
            try {
                parser.parse(Files.readString(file));
            } catch (RuntimeException ex) {
                failures.add(root.relativize(file) + " -> " + ex.getMessage());
            }
        }

        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    void authoredUiHtmlResourcesStayRecoverable() throws IOException {
        Path root = workspaceRoot();
        UiMarkupParser parser = new UiMarkupParser();
        ArrayList<String> failures = new ArrayList<>();
        List<String> forbidden = List.of("on" + "click", "on" + "mouseover", "on" + "load", "javascript:" );

        for (Path file : authoredResources(root, List.of(".html"))) {
            String source = Files.readString(file);
            String normalized = source.toLowerCase(Locale.ROOT);
            for (String token : forbidden) {
                if (normalized.contains(token)) {
                    failures.add(root.relativize(file) + " -> forbidden browser-style token " + token);
                }
            }
            try {
                parser.parse(source);
            } catch (RuntimeException ex) {
                failures.add(root.relativize(file) + " -> " + ex.getMessage());
            }
        }

        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    private static List<Path> authoredResources(Path root, List<String> suffixes) throws IOException {
        ArrayList<Path> files = new ArrayList<>();
        for (Path base : List.of(
                root.resolve("modulesSrc/engine-ui/src/main/resources/engine-ui/styles"),
                root.resolve("modulesSrc/engine-ui/src/main/resources/engine-ui/devtools/ui"),
                root.resolve("editor/src/main/resources/editor/ui"),
                root.resolve("gameTypes/bootSequenceEditor/src/main/resources"),
                root.resolve("assets/topdown/ui")
        )) {
            if (!Files.exists(base)) continue;
            try (var stream = Files.walk(base)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> !path.toString().replace('\\', '/').contains("/build/"))
                        .filter(path -> suffixes.stream().anyMatch(suffix -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(suffix)))
                        .forEach(files::add);
            }
        }
        return files;
    }

    private static Path workspaceRoot() {
        Path cursor = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && cursor != null; i++) {
            if (Files.exists(cursor.resolve("settings.gradle")) && Files.exists(cursor.resolve("modulesSrc"))) return cursor;
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Cannot resolve java-platformer workspace root");
    }
}
