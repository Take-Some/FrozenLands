package dev.takesome.helix.ui.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Architectural guard for the Lua-driven UI binding boundary.
 *
 * <p>The engine UI runtime may know descriptor fields such as id, target,
 * source, type, expr and format. It must not know concrete game state paths
 * such as player XP, health, enemy HP or inventory slots.</p>
 */
public final class NoDomainUiBindingHardcodeInEngineUiTest {
    private static final List<String> FORBIDDEN_UI_DOMAIN_TOKENS = List.of(
            "player.xp",
            "player.xpNext",
            "player.xpRatio",
            "player.health",
            "enemy.hp",
            "inventory.slots",
            "bindXp",
            "bindPlayerXp",
            "xpText",
            "xpBar"
    );

    @Test
    public void engineUiJavaSourcesMustNotContainDomainUiBindings() throws IOException {
        Path root = requiredProjectPath("src/main/java");

        List<String> violations = new ArrayList<>();
        scanRoot(root, violations);

        assertTrue(
                violations.isEmpty(),
                () -> "Engine UI Java sources must not contain game-specific UI binding hardcode. Violations:\n"
                        + String.join("\n", violations)
        );
    }

    private static void scanRoot(Path root, List<String> violations) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path file : files) {
                scanFile(file, violations);
            }
        }
    }

    private static void scanFile(Path file, List<String> violations) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);

        for (String token : FORBIDDEN_UI_DOMAIN_TOKENS) {
            int index = content.indexOf(token);
            if (index >= 0) {
                violations.add(file + ":" + lineOf(content, index) + " contains forbidden UI domain token `" + token + "`");
            }
        }
    }

    private static int lineOf(String content, int index) {
        int line = 1;
        for (int i = 0; i < index; i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static Path requiredProjectPath(String relativePath) {
        Path path = optionalProjectPath(relativePath);
        if (path == null) {
            fail("Cannot resolve engine-ui project path: " + relativePath);
        }
        return path;
    }

    private static Path optionalProjectPath(String relativePath) {
        for (Path candidate : projectPathCandidates(relativePath)) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static List<Path> projectPathCandidates(String relativePath) {
        return List.of(
                Path.of(relativePath),
                Path.of("modulesSrc/engine-ui").resolve(relativePath),
                Path.of("javaEngine/Java2DGame/modulesSrc/engine-ui").resolve(relativePath)
        );
    }
}
