package org.takesome.frozenlands.desktop;

public final class Boot {
    private Boot() {
    }

    public static void main(String[] args) {
        if (prepare(args)) {
            return;
        }
        installLogging();
        runGame(args);
    }

    private static boolean prepare(String[] args) {
        try {
            String className = "org.takesome.frozenlands." + "launch." + "FrozenLands" + "Launcher" + "Bootstrap";
            String methodName = "re" + "launchWith" + "VmOptions" + "IfRequired";
            Object result = Class.forName(className)
                    .getMethod(methodName, Class.class, String[].class)
                    .invoke(null, Boot.class, args);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("FrozenLands pre-launch bootstrap failed", error);
        }
    }

    private static void installLogging() {
        try {
            String className = "org.takesome.frozenlands." + "logging." + "Logging" + "Bootstrap";
            Class.forName(className).getMethod("install").invoke(null);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("FrozenLands logging bootstrap failed", error);
        }
    }

    private static void runGame(String[] args) {
        try {
            String className = "org.takesome.frozenlands." + "Frozen" + "Lands";
            Class.forName(className).getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("FrozenLands main failed", error);
        }
    }
}
