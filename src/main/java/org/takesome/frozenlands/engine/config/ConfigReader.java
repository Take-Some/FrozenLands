package org.takesome.frozenlands.engine.config;

import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigReader {
    private static final String BOOTSTRAP_MODULE = "engine.bootstrap";
    private static final ModuleIndexCatalog MODULE_INDEX = ModuleIndexCatalog.defaultCatalog();

    private final Map<String, Map> configMaps = new LinkedHashMap<>();

    public ConfigReader(String[] configFiles) {
        for (String configFile : configFiles) {
            configMaps.put(configFile, readConfig(configFile));
        }
    }

    public Map<String, Map> getCfgMaps() {
        return configMaps;
    }

    private Map<String, Object> readConfig(String configName) {
        return MODULE_INDEX.readConfigMap(BOOTSTRAP_MODULE, configName);
    }
}
