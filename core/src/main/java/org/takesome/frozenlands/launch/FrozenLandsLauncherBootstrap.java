package org.takesome.frozenlands.launch;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class FrozenLandsLauncherBootstrap {
    private FrozenLandsLauncherBootstrap() {
    }

    public static boolean relaunchWithVmOptionsIfRequired(Class<?> mainClass, String[] args) {
        if (mainClass == null) {
            throw new IllegalArgumentException("mainClass must not be null");
        }
        if (Boolean.getBoolean(FrozenLandsLaunchProperties.RELAUNCHED_FLAG)) {
            return false;
        }

        FrozenLandsLaunchContext context = FrozenLandsLaunchContext.create(mainClass);
        Optional<Path> file = new FrozenLandsVmOptionsLocator().locate(context);
        if (file.isEmpty()) {
            if (FrozenLandsLaunchProperties.verbose()) {
                FrozenLandsLaunchLog.info("No vmoptions file found under launcher root: " + context.rootDirectory());
            }
            return false;
        }

        List<String> options = new FrozenLandsVmOptionsParser().read(file.get());
        if (options.isEmpty()) {
            FrozenLandsLaunchLog.info("VM options file is present but has no active options: " + file.get());
            return false;
        }

        return new FrozenLandsJvmRelauncher().relaunch(
                context,
                file.get(),
                options,
                args == null ? new String[0] : args
        );
    }
}
