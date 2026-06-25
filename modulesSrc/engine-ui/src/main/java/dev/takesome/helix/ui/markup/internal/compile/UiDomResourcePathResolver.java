package dev.takesome.helix.ui.markup.internal.compile;

import dev.takesome.helix.data.io.DataFiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

/** Resolves document-local resource references used by HTML-like markup. */
final class UiDomResourcePathResolver {
    private static final Pattern WINDOWS_DRIVE = Pattern.compile("^[A-Za-z]:/.*");

    private UiDomResourcePathResolver() {
    }

    static String resolve(String path, String sourcePath) {
        String rawPath = path == null ? "" : path.trim().replace('\\', '/');
        if (rawPath.isBlank()) return "";
        if (rawPath.contains("://")) return rawPath;
        if (rawPath.startsWith("/")) return DataFiles.normalize(rawPath);

        String cleanPath = DataFiles.normalize(rawPath);
        if (isAbsoluteResourcePath(cleanPath)) return cleanPath;

        String cleanSource = DataFiles.normalize(sourcePath);
        if (cleanSource.isBlank()) return cleanPath;
        int slash = cleanSource.lastIndexOf('/');
        if (slash < 0) return cleanPath;
        return normalizeRelativePath(cleanSource.substring(0, slash + 1) + cleanPath);
    }

    private static boolean isAbsoluteResourcePath(String path) {
        if (path == null || path.isBlank()) return false;
        return WINDOWS_DRIVE.matcher(path).matches() || Path.of(path).isAbsolute();
    }

    private static String normalizeRelativePath(String path) {
        String normalized = DataFiles.normalize(path);
        ArrayList<String> parts = new ArrayList<>();
        String prefix = "";
        if (WINDOWS_DRIVE.matcher(normalized).matches()) {
            prefix = normalized.substring(0, 3);
            normalized = normalized.substring(3);
        }
        for (String part : normalized.split("/")) {
            if (part.isBlank() || ".".equals(part)) continue;
            if ("..".equals(part)) {
                if (!parts.isEmpty()) parts.remove(parts.size() - 1);
                continue;
            }
            parts.add(part);
        }
        return prefix + String.join("/", parts);
    }
}
