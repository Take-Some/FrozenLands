package org.takesome.frozenlands.launch;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

final class FrozenLandsJvmInputArguments {
    List<String> collect() {
        if (!FrozenLandsLaunchProperties.preserveInputArguments()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (argument == null || argument.isBlank()) {
                continue;
            }
            if (isRelaunchFlag(argument)) {
                continue;
            }
            result.add(argument);
        }
        return List.copyOf(result);
    }

    private boolean isRelaunchFlag(String argument) {
        String prefix = "-D" + FrozenLandsLaunchProperties.RELAUNCHED_FLAG;
        return argument.equals(prefix) || argument.startsWith(prefix + "=");
    }
}
