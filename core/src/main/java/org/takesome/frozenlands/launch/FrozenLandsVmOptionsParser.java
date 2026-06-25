package org.takesome.frozenlands.launch;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class FrozenLandsVmOptionsParser {
    List<String> read(Path file) {
        List<String> result = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String option = normalize(line, lineNumber);
                if (!option.isEmpty()) {
                    result.add(option);
                }
            }
        } catch (Exception error) {
            FrozenLandsLaunchLog.warn("Failed to read vmoptions file: " + file, error);
        }

        return List.copyOf(result);
    }

    private String normalize(String rawLine, int lineNumber) {
        String line = rawLine == null ? "" : rawLine;
        if (lineNumber == 1 && line.startsWith("\uFEFF")) {
            line = line.substring(1);
        }

        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return "";
        }

        return stripInlineComment(line).trim();
    }

    private String stripInlineComment(String line) {
        boolean doubleQuoted = false;
        boolean escaped = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                doubleQuoted = !doubleQuoted;
                continue;
            }
            if (ch == '#' && !doubleQuoted && isCommentStart(line, i)) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private boolean isCommentStart(String line, int index) {
        return index == 0 || Character.isWhitespace(line.charAt(index - 1));
    }
}
