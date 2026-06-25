package org.takesome.frozenlands.engine.lua;

import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LuaModuleApiCatalog {
    private final ModuleIndexCatalog moduleIndexCatalog = ModuleIndexCatalog.defaultCatalog();

    public String pathFor(String moduleId) {
        return moduleIndexCatalog.luaPath(moduleId, "api");
    }

    public String eventsPathFor(String moduleId) {
        return moduleIndexCatalog.luaPath(moduleId, "events");
    }

    public String readApi(String moduleId) {
        return moduleIndexCatalog.readLua(moduleId, "api");
    }

    public String readEventsApi(String moduleId) {
        return moduleIndexCatalog.readLua(moduleId, "events");
    }

    public Map<String, String> readApis(Iterable<String> moduleIds) {
        Map<String, String> apis = new LinkedHashMap<>();
        for (String moduleId : moduleIds) {
            apis.put(moduleId, readApi(moduleId));
        }
        return apis;
    }

    public Map<String, String> readEventApis(Iterable<String> moduleIds) {
        Map<String, String> apis = new LinkedHashMap<>();
        for (String moduleId : moduleIds) {
            apis.put(moduleId, readEventsApi(moduleId));
        }
        return apis;
    }
}
