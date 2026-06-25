package org.takesome.frozenlands.engine.core.console;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

final class ConsoleArgumentParser {
    private final ObjectMapper mapper = new ObjectMapper();

    ConsoleRequest parse(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isBlank()) {
            return new ConsoleRequest("", "", Map.of());
        }
        int separator = firstWhitespace(trimmed);
        String command = separator < 0 ? trimmed : trimmed.substring(0, separator);
        String rawArguments = separator < 0 ? "" : trimmed.substring(separator + 1).trim();
        return new ConsoleRequest(command, rawArguments, parseArguments(rawArguments));
    }

    Map<String, Object> parseArguments(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return parseJson(trimmed);
        }
        return parseKeyValues(trimmed);
    }

    private Map<String, Object> parseJson(String raw) {
        try {
            return mapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (IOException e) {
            throw new IllegalArgumentException("Console JSON arguments are invalid: " + raw, e);
        }
    }

    private Map<String, Object> parseKeyValues(String raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String token : raw.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            int index = token.indexOf('=');
            if (index <= 0) {
                result.put("value", raw);
                return result;
            }
            String key = token.substring(0, index);
            String value = token.substring(index + 1);
            result.put(key, scalar(value));
        }
        return result;
    }

    private Object scalar(String value) {
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private int firstWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
