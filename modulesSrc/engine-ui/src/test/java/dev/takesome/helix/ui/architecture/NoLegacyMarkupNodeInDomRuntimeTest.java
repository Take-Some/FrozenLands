package dev.takesome.helix.ui.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class NoLegacyMarkupNodeInDomRuntimeTest {
    private static final List<String> DOM_RUNTIME_ROOTS = List.of(
            "src/main/java/dev/takesome/helix/ui/markup/internal/compile",
            "src/main/java/dev/takesome/helix/ui/markup/internal/factory",
            "src/main/java/dev/takesome/helix/ui/markup/internal/module",
            "src/main/java/dev/takesome/helix/ui/markup/internal/model",
            "src/main/java/dev/takesome/helix/ui/markup/internal/i18n"
    );

    @Test
    void domRuntimeMustNotDependOnLegacyMarkupNode() throws IOException {
        List<String> forbiddenSymbols = List.of(
                "Ui" + "Markup" + "Node",
                "Ui" + "Markup" + "Node" + "Factory",
                "Ui" + "Markup" + "Attributes",
                "Ui" + "Markup" + "I18n" + "Text",
                "Ui" + "Markup" + "Module" + "Gate",
                "Ui" + "Markup" + "Compilation" + "Pipeline",
                "Ui" + "Markup" + "Dom" + "Style" + "Bridge",
                "Ui" + "Markup" + "Computed" + "Styles",
                "Ui" + "Markup" + "Resource" + "Path" + "Resolver",
                "Ui" + "Markup" + "Style" + "Reader",
                "Ui" + "Markup" + "Layout" + "Resolver",
                "Ui" + "Markup" + "Button" + "Skin" + "Resolver",
                "Ui" + "Markup" + "Skin" + "Resolver",
                "Ui" + "Markup" + "Root" + "Decorator"
        );
        StringBuilder violations = new StringBuilder();

        for (String rootPath : DOM_RUNTIME_ROOTS) {
            Path root = sourcePath(rootPath);
            if (!Files.exists(root)) continue;
            try (Stream<Path> stream = Files.walk(root)) {
                for (Path file : stream.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String content = Files.readString(file);
                    for (String symbol : forbiddenSymbols) {
                        if (content.contains(symbol)) {
                            violations.append(file).append(" contains ").append(symbol).append(System.lineSeparator());
                        }
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "DOM runtime must not depend on legacy markup node/factory symbols:\n" + violations
        );
    }

    private static Path sourcePath(String moduleRelativePath) {
        Path path = Path.of(moduleRelativePath);
        if (!Files.exists(path)) path = Path.of("modulesSrc/engine-ui").resolve(moduleRelativePath);
        return path;
    }
}
