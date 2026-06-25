package org.takesome.frozenlands.launch;

import java.nio.file.Files;
import java.nio.file.Path;

final class FrozenLandsJavaExecutable {
    private FrozenLandsJavaExecutable() {
    }

    static Path resolve() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String executableName = windows ? "java.exe" : "java";
        String javaHome = System.getProperty("java.home", "");

        if (!javaHome.isBlank()) {
            Path candidate = Path.of(javaHome, "bin", executableName).toAbsolutePath().normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        return Path.of(executableName);
    }
}
