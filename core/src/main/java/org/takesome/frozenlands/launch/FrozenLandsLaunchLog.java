package org.takesome.frozenlands.launch;

final class FrozenLandsLaunchLog {
    private static final String PREFIX = "[FrozenLands Launcher] ";

    private FrozenLandsLaunchLog() {
    }

    static void info(String message) {
        System.out.println(PREFIX + message);
    }

    static void warn(String message) {
        System.err.println(PREFIX + message);
    }

    static void warn(String message, Throwable throwable) {
        warn(message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }
}
