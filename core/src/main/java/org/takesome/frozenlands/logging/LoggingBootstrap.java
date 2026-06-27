package org.takesome.frozenlands.logging;

import org.fusesource.jansi.AnsiConsole;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class LoggingBootstrap {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private LoggingBootstrap() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        setDefaultProperty("log4j2.skipJansi", "false");
        setDefaultProperty("log4j.skipJansi", "false");
        setDefaultProperty("log4j2.enableJansi", "true");
        setDefaultProperty("log4j2.forceAnsi", "true");
        setDefaultProperty("jansi.mode", "force");
        setDefaultProperty("jansi.out.mode", "force");
        setDefaultProperty("jansi.err.mode", "force");
        setDefaultProperty("jansi.passthrough", "true");
        setDefaultProperty("jansi.strip", "false");
        setDefaultProperty("frozenlands.external.log.level", "error");
        setDefaultProperty("frozenlands.log.level", "info");
        AnsiConsole.systemInstall();
        Runtime.getRuntime().addShutdownHook(new Thread(AnsiConsole::systemUninstall, "jansi-shutdown"));
        emitAnsiProbeIfRequested();
        disableKnownNativeStartupMessages();

        resetJulHandlers();
        configureExternalLoggers();
        SLF4JBridgeHandler.install();
    }

    private static void setDefaultProperty(String name, String value) {
        if (System.getProperty(name) == null) {
            System.setProperty(name, value);
        }
    }

    private static void emitAnsiProbeIfRequested() {
        if (!Boolean.parseBoolean(System.getProperty("frozenlands.ansi.probe", "false"))) {
            return;
        }
        System.out.println("[90m[FrozenLands ANSI][0m "
                + "[32mGREEN[0m "
                + "[33mYELLOW[0m "
                + "[31mRED[0m "
                + "[36mCYAN[0m");
    }

    private static void disableKnownNativeStartupMessages() {
        invokeIfPresent("com.jme3.bullet.util.NativeLibrary", "setStartupMessageEnabled", false);
        invokeIfPresent("com.jme3.bullet.util.NativeLibrary", "setStartMessageEnabled", false);
        invokeIfPresent("com.jme3.bullet.util.NativeLibrary", "setLoadMessageEnabled", false);
    }

    private static void invokeIfPresent(String className, String methodName, boolean value) {
        try {
            Class<?> type = Class.forName(className);
            type.getMethod(methodName, boolean.class).invoke(null, value);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private static void resetJulHandlers() {
        try {
            LogManager.getLogManager().reset();
        } catch (SecurityException ignored) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
        }
    }

    private static void configureExternalLoggers() {
        setJulLevel("", Level.INFO);
        setJulLevel("com.jme3", Level.SEVERE);
        setJulLevel("jme3utilities", Level.SEVERE);
        setJulLevel("org.lwjgl", Level.SEVERE);
        setJulLevel("com.github.stephengold", Level.SEVERE);
        setJulLevel("com.simsilica", Level.SEVERE);
    }

    private static void setJulLevel(String loggerName, Level level) {
        Logger logger = Logger.getLogger(loggerName);
        logger.setLevel(level);
    }
}
