package org.takesome.frozenlands.launch;

import java.util.List;

final class FrozenLandsLaunchProperties {
    static final String RELAUNCHED_FLAG = "frozenlands.launcher.relaunched";

    static final String ROOT_PROPERTY = "frozenlands.launch.root";
    static final String LEGACY_ROOT_PROPERTY = "helix.launch.root";
    static final String VMOPTIONS_PATH_PROPERTY = "frozenlands.vmoptions";
    static final String LEGACY_VMOPTIONS_PATH_PROPERTY = "helix.vmoptions";
    static final String VMOPTIONS_NAME_PROPERTY = "frozenlands.vmoptions.name";
    static final String PRESERVE_INPUT_ARGS_PROPERTY = "frozenlands.launcher.preserveInputArgs";
    static final String VERBOSE_PROPERTY = "frozenlands.launcher.verbose";

    private static final List<String> DEFAULT_VMOPTIONS_FILE_NAMES = List.of(
            "FrozenLands.vmoptions",
            "FROZENLANDS.vmoptions",
            "frozenlands.vmoptions",
            "Helix.vmoptions",
            "HELIX.vmoptions",
            "helix.vmoptions",
            "vmoptions"
    );

    private FrozenLandsLaunchProperties() {
    }

    static List<String> vmOptionsFileNames() {
        String configured = System.getProperty(VMOPTIONS_NAME_PROPERTY, "");
        if (configured.isBlank()) {
            return DEFAULT_VMOPTIONS_FILE_NAMES;
        }
        return List.of(configured.trim());
    }

    static boolean preserveInputArguments() {
        return booleanProperty(PRESERVE_INPUT_ARGS_PROPERTY, true);
    }

    static boolean verbose() {
        return booleanProperty(VERBOSE_PROPERTY, false);
    }

    private static boolean booleanProperty(String name, boolean fallback) {
        String value = System.getProperty(name, "");
        return value.isBlank() ? fallback : Boolean.parseBoolean(value.trim());
    }
}
