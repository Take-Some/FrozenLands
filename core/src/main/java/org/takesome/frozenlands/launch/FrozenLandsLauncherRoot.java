package org.takesome.frozenlands.launch;

import java.nio.file.Path;

final class FrozenLandsLauncherRoot {
    private FrozenLandsLauncherRoot() {
    }

    static Path resolve() {
        String explicit = firstSystemProperty(FrozenLandsLaunchProperties.ROOT_PROPERTY, FrozenLandsLaunchProperties.LEGACY_ROOT_PROPERTY);
        if (!explicit.isBlank()) {
            return absolute(explicit);
        }

        String assetsRoot = System.getProperty("assets.rootDir", "");
        if (!assetsRoot.isBlank()) {
            Path assets = absolute(assetsRoot);
            Path parent = assets.getParent();
            return parent == null ? assets : parent;
        }

        String gameAssetsDir = System.getProperty("game.assets.dir", "");
        if (!gameAssetsDir.isBlank()) {
            Path gameAssets = absolute(gameAssetsDir);
            Path assets = gameAssets.getParent();
            if (assets != null && assets.getParent() != null) {
                return assets.getParent();
            }
            return gameAssets;
        }

        return fromWorkingDirectory();
    }

    private static String firstSystemProperty(String primary, String fallback) {
        String value = System.getProperty(primary, "");
        return value.isBlank() ? System.getProperty(fallback, "") : value;
    }

    private static Path fromWorkingDirectory() {
        Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        String cwdName = fileName(cwd);
        Path parent = cwd.getParent();

        if ("assets".equalsIgnoreCase(cwdName) && parent != null) {
            return parent;
        }
        if (parent != null && "assets".equalsIgnoreCase(fileName(parent))) {
            Path workspace = parent.getParent();
            return workspace == null ? parent : workspace;
        }
        return cwd;
    }

    private static Path absolute(String raw) {
        return Path.of(raw).toAbsolutePath().normalize();
    }

    private static String fileName(Path path) {
        Path name = path == null ? null : path.getFileName();
        return name == null ? "" : name.toString();
    }
}
