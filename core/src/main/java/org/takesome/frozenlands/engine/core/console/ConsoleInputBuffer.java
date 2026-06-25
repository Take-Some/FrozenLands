package org.takesome.frozenlands.engine.core.console;

final class ConsoleInputBuffer {
    private final StringBuilder line = new StringBuilder();

    String line() {
        return line.toString();
    }

    boolean isBlank() {
        return line.toString().isBlank();
    }

    void setLine(String value) {
        line.setLength(0);
        if (value != null) {
            line.append(value);
        }
    }

    void clear() {
        line.setLength(0);
    }

    void append(char character) {
        if (character >= 32 && character != 127) {
            line.append(character);
        }
    }

    void backspace() {
        if (!line.isEmpty()) {
            line.deleteCharAt(line.length() - 1);
        }
    }

    String commandToken() {
        String value = line.toString().trim();
        int whitespace = firstWhitespace(value);
        return whitespace < 0 ? value : value.substring(0, whitespace);
    }

    void replaceCommandToken(String command, boolean appendSpace) {
        String current = line.toString();
        int whitespace = firstWhitespace(current);
        String tail = whitespace < 0 ? "" : current.substring(whitespace);
        line.setLength(0);
        line.append(command == null ? "" : command);
        if (appendSpace && tail.isBlank()) {
            line.append(' ');
        } else {
            line.append(tail);
        }
    }

    private int firstWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return index;
            }
        }
        return -1;
    }
}
