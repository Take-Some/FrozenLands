package org.takesome.frozenlands.launch;

import java.util.ArrayList;
import java.util.List;

public final class FrozenLandsPreLaunchBootstrap {
    private FrozenLandsPreLaunchBootstrap() {
    }

    public static boolean prepare(Class<?> mainClass, String[] args) {
        return FrozenLandsLauncherBootstrap.relaunchWithVmOptionsIfRequired(mainClass, args);
    }

    public static boolean prepareCurrentProcess() {
        if (Boolean.getBoolean(FrozenLandsLaunchProperties.RELAUNCHED_FLAG)) {
            return false;
        }

        List<String> command = splitCommandLine(System.getProperty("sun.java.command", ""));
        if (command.isEmpty()) {
            return false;
        }

        String mainClassName = command.get(0);
        if (mainClassName.endsWith(".jar")) {
            return false;
        }

        try {
            Class<?> mainClass = Class.forName(mainClassName);
            String[] args = command.subList(1, command.size()).toArray(String[]::new);
            return prepare(mainClass, args);
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static List<String> splitCommandLine(String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return List.of();
        }

        ArrayList<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < commandLine.length(); i++) {
            char ch = commandLine.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                quoted = !quoted;
                continue;
            }
            if (Character.isWhitespace(ch) && !quoted) {
                flushToken(result, current);
                continue;
            }
            current.append(ch);
        }
        flushToken(result, current);
        return List.copyOf(result);
    }

    private static void flushToken(List<String> result, StringBuilder current) {
        if (current.isEmpty()) {
            return;
        }
        result.add(current.toString());
        current.setLength(0);
    }
}
