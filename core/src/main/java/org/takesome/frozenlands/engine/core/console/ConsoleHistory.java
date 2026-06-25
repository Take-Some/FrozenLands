package org.takesome.frozenlands.engine.core.console;

import java.util.ArrayList;
import java.util.List;

final class ConsoleHistory {
    private final List<String> entries = new ArrayList<>();
    private int cursor = -1;

    void push(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        if (entries.isEmpty() || !entries.get(entries.size() - 1).equals(line)) {
            entries.add(line);
        }
        cursor = -1;
    }

    String previous(String current) {
        if (entries.isEmpty()) {
            return current;
        }
        if (cursor < 0) {
            cursor = entries.size() - 1;
        } else if (cursor > 0) {
            cursor--;
        }
        return entries.get(cursor);
    }

    String next(String current) {
        if (entries.isEmpty() || cursor < 0) {
            return current;
        }
        if (cursor < entries.size() - 1) {
            cursor++;
            return entries.get(cursor);
        }
        cursor = -1;
        return "";
    }
}
