package dev.takesome.helix.ui.architecture;




import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import org.junit.jupiter.api.Test;



import java.io.IOException;

import java.nio.file.Files;

import java.nio.file.Path;

import java.util.List;

import java.util.Set;

import java.util.stream.Stream;



import static org.junit.jupiter.api.Assertions.assertTrue;



/** Guard against adding new engine-ui monoliths. Existing large files remain explicit split debt. */

final class EngineUiMonolithGuardTest {

    private static final int MAX_LINES_WITHOUT_ALLOWLIST = 500;

    private static final Set<String> ALLOWED_SPLIT_DEBT = Set.of(

            normalized("dev/takesome/helix/ui/components/UiSettingsFieldsNode.java"),

            normalized("dev/takesome/helix/ui/css/UiCssLayoutEngine.java")

    );



    @Test

    void engineUiMustNotAddNewFilesOverFiveHundredLines() throws IOException {

        Path sourceRoot = projectPath("src/main/java");

        Path uiRoot = sourceRoot.resolve("dev/takesome/helix/ui");

        List<String> violations;

        try (Stream<Path> stream = Files.walk(uiRoot)) {

            violations = stream

                    .filter(Files::isRegularFile)

                    .filter(path -> path.toString().endsWith(".java"))

                    .map(path -> violation(sourceRoot, path))

                    .filter(value -> !value.isBlank())

                    .toList();

        }



        assertTrue(

                violations.isEmpty(),

                () -> "New engine-ui monolith candidates found. Split model/layout/render/input/factory first:\n"

                        + String.join("\n", violations)

        );

    }



    private static String violation(Path sourceRoot, Path path) {

        try (Stream<String> lines = Files.lines(path)) {

            long count = lines.count();

            String modulePath = normalized(sourceRoot.relativize(path).toString());

            if (count <= MAX_LINES_WITHOUT_ALLOWLIST || ALLOWED_SPLIT_DEBT.contains(modulePath)) return "";

            return count + " lines: " + modulePath;

        } catch (IOException ex) {

            return "Cannot read " + path + ": " + ex.getMessage();

        }

    }



    private static Path projectPath(String relativePath) {

        for (Path candidate : List.of(

                Path.of(relativePath),

                Path.of("modulesSrc/engine-ui").resolve(relativePath),

                Path.of("javaEngine/Java2DGame/modulesSrc/engine-ui").resolve(relativePath)

        )) {

            if (Files.exists(candidate)) return candidate;

        }

        throw new IllegalStateException("Cannot resolve engine-ui path: " + relativePath);

    }



    private static String normalized(String value) {

        return emptyIfNull(value).replace('\\', '/');

    }

}
