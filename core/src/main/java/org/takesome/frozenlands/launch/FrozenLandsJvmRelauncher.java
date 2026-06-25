package org.takesome.frozenlands.launch;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class FrozenLandsJvmRelauncher {
    boolean relaunch(FrozenLandsLaunchContext context, Path vmOptionsFile, List<String> vmOptions, String[] applicationArgs) {
        LaunchArguments launchArguments = splitLaunchArguments(vmOptions);
        if (launchArguments.empty()) {
            return false;
        }

        try {
            List<String> command = buildCommand(context, launchArguments, applicationArgs);
            FrozenLandsLaunchLog.info("Relaunching JVM with vmoptions file: " + vmOptionsFile);
            FrozenLandsLaunchLog.info("Launcher root: " + context.rootDirectory());
            FrozenLandsLaunchLog.info("VM option count: " + launchArguments.jvmOptions().size());
            for (String option : launchArguments.jvmOptions()) {
                FrozenLandsLaunchLog.info("Applying JVM option: " + option);
            }
            if (!launchArguments.applicationArgs().isEmpty()) {
                FrozenLandsLaunchLog.info("Application arg count from vmoptions: " + launchArguments.applicationArgs().size());
                for (String arg : launchArguments.applicationArgs()) {
                    FrozenLandsLaunchLog.info("Forwarding application arg from vmoptions: " + arg);
                }
            }

            Process process = new ProcessBuilder(command).inheritIO().start();
            int exitCode = process.waitFor();
            System.exit(exitCode);
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            FrozenLandsLaunchLog.warn("Relaunch interrupted; continuing in current JVM without external vmoptions.", interrupted);
            return false;
        } catch (Exception error) {
            FrozenLandsLaunchLog.warn("Relaunch failed; continuing in current JVM without external vmoptions.", error);
            return false;
        }
    }

    private List<String> buildCommand(FrozenLandsLaunchContext context, LaunchArguments launchArguments, String[] applicationArgs) {
        List<String> command = new ArrayList<>();
        command.add(context.javaExecutable().toString());
        command.addAll(new FrozenLandsJvmInputArguments().collect());
        command.addAll(launchArguments.jvmOptions());
        command.add("-D" + FrozenLandsLaunchProperties.RELAUNCHED_FLAG + "=true");
        command.add("-D" + FrozenLandsLaunchProperties.ROOT_PROPERTY + "=" + context.rootDirectory());
        command.add("-cp");
        command.add(context.classPath());
        command.add(context.mainClassName());
        command.addAll(launchArguments.applicationArgs());
        if (applicationArgs != null && applicationArgs.length > 0) {
            command.addAll(Arrays.asList(applicationArgs));
        }
        return command;
    }

    private LaunchArguments splitLaunchArguments(List<String> rawOptions) {
        ArrayList<String> jvmOptions = new ArrayList<>();
        ArrayList<String> applicationArgs = new ArrayList<>();
        if (rawOptions != null) {
            for (String raw : rawOptions) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String value = raw.trim();
                if (value.startsWith("--")) {
                    applicationArgs.add(value);
                } else {
                    jvmOptions.add(value);
                }
            }
        }
        return new LaunchArguments(List.copyOf(jvmOptions), List.copyOf(applicationArgs));
    }

    private record LaunchArguments(List<String> jvmOptions, List<String> applicationArgs) {
        boolean empty() {
            return jvmOptions.isEmpty() && applicationArgs.isEmpty();
        }
    }
}
