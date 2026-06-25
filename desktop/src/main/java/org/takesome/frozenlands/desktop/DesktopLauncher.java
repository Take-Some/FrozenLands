package org.takesome.frozenlands.desktop;

import org.takesome.frozenlands.FrozenLands;
import org.takesome.frozenlands.logging.LoggingBootstrap;

public final class DesktopLauncher {
    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        LoggingBootstrap.install();
        FrozenLands.main(args);
    }
}
