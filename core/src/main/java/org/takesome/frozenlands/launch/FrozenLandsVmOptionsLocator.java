package org.takesome.frozenlands.launch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class FrozenLandsVmOptionsLocator {
    Optional<Path> locate(FrozenLandsLaunchContext context) {
        String explicitPath = firstSystemProperty(
                FrozenLandsLaunchProperties.VMOPTIONS_PATH_PROPERTY,
                FrozenLandsLaunchProperties.LEGACY_VMOPTIONS_PATH_PROPERTY
        );
        if (!explicitPath.isBlank()) {
            Path path = resolveAgainstRoot(context, explicitPath.trim());
            if (Files.isRegularFile(path)) {
                return Optional.of(path);
            }
            FrozenLandsLaunchLog.warn("Explicit vmoptions file does not exist: " + path);
            return Optional.empty();
        }

        for (String fileName : FrozenLandsLaunchProperties.vmOptionsFileNames()) {
            if (fileName == null || fileName.isBlank()) {
                continue;
            }
            Path candidate = context.rootDirectory()
                    .resolve(fileName.trim())
                    .toAbsolutePath()
                    .normalize();
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private String firstSystemProperty(String primary, String fallback) {
        String value = System.getProperty(primary, "");
        return value.isBlank() ? System.getProperty(fallback, "") : value;
    }

    private Path resolveAgainstRoot(FrozenLandsLaunchContext context, String rawPath) {
        Path path = Path.of(rawPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return context.rootDirectory().resolve(path).toAbsolutePath().normalize();
    }
}
