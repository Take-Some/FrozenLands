package org.takesome.frozenlands.launch;

import java.nio.file.Path;
import java.util.Objects;

final class FrozenLandsLaunchContext {
    private final Class<?> mainClass;
    private final Path rootDirectory;
    private final Path javaExecutable;
    private final String classPath;

    private FrozenLandsLaunchContext(Class<?> mainClass, Path rootDirectory, Path javaExecutable, String classPath) {
        this.mainClass = Objects.requireNonNull(mainClass, "mainClass");
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        this.javaExecutable = Objects.requireNonNull(javaExecutable, "javaExecutable");
        this.classPath = classPath == null ? "" : classPath;
    }

    static FrozenLandsLaunchContext create(Class<?> mainClass) {
        return new FrozenLandsLaunchContext(
                mainClass,
                FrozenLandsLauncherRoot.resolve(),
                FrozenLandsJavaExecutable.resolve(),
                System.getProperty("java.class.path", "")
        );
    }

    String mainClassName() { return mainClass.getName(); }
    Path rootDirectory() { return rootDirectory; }
    Path javaExecutable() { return javaExecutable; }
    String classPath() { return classPath; }
}
