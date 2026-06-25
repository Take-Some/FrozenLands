package org.takesome.frozenlands.logging;

import org.fusesource.jansi.AnsiConsole;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.concurrent.atomic.AtomicBoolean;

public final class LoggingBootstrap {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private LoggingBootstrap() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        System.setProperty("log4j2.skipJansi", "false");
        System.setProperty("log4j.skipJansi", "false");
        AnsiConsole.systemInstall();
        Runtime.getRuntime().addShutdownHook(new Thread(AnsiConsole::systemUninstall, "jansi-shutdown"));

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
