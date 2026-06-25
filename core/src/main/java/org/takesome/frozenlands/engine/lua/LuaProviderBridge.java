package org.takesome.frozenlands.engine.lua;

import org.takesome.frozenlands.engine.EngineContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LuaProviderBridge {
    private final EngineContext context;
    private final LuaModuleApiCatalog luaApiCatalog = new LuaModuleApiCatalog();

    public LuaProviderBridge(EngineContext context) {
        this.context = context;
    }

    public Map<String, Object> exportProviders() {
        return context.getProviders().luaManifest();
    }

    public Map<String, Object> exportModules() {
        return context.getModuleRegistry().luaManifest();
    }

    public Map<String, Object> exportRuntimeManifest() {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("providers", exportProviders());
        manifest.put("modules", exportModules());
        return manifest;
    }

    public String exportModuleApi(String moduleId) {
        return luaApiCatalog.readApi(moduleId);
    }

    public String exportModuleEventsApi(String moduleId) {
        return luaApiCatalog.readEventsApi(moduleId);
    }

    public Map<String, String> exportAllModuleApis() {
        return luaApiCatalog.readApis(context.getModuleRegistry().snapshot().keySet());
    }

    public Map<String, String> exportAllModuleEventApis() {
        return luaApiCatalog.readEventApis(context.getModuleRegistry().snapshot().keySet());
    }

    public Object requireProvider(String id) {
        return context.getProviderRegistry().require(id);
    }

    public Map<String, Object> callProvider(String providerId, String commandId, Map<String, Object> arguments) {
        return context.getProviderRegistry().call(providerId, commandId, arguments);
    }

    public Map<String, Object> publishProviderEvent(String topic, Map<String, Object> payload) {
        return context.getProviderRegistry().publishEvent(topic, payload);
    }

    public Map<String, Object> callModule(String moduleId, String commandId, Map<String, Object> arguments) {
        return context.getModuleRegistry().call(moduleId, commandId, arguments);
    }

    public Map<String, Object> publishModuleEvent(String topic, Map<String, Object> payload) {
        return context.getModuleRegistry().publishEvent(topic, payload);
    }

    public List<Map<String, Object>> drainJavaEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        addEvents(events, "module", context.getModuleRegistry().getEventBus().drain());
        addEvents(events, "provider", context.getProviderRegistry().getEventBus().drain());
        return events;
    }

    public List<Map<String, Object>> snapshotJavaEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        addEvents(events, "module", context.getModuleRegistry().getEventBus().snapshot());
        addEvents(events, "provider", context.getProviderRegistry().getEventBus().snapshot());
        return events;
    }

    public List<Map<String, Object>> drainModuleEvents() {
        return context.getModuleRegistry().getEventBus().drain();
    }

    public List<Map<String, Object>> snapshotModuleEvents() {
        return context.getModuleRegistry().getEventBus().snapshot();
    }

    public List<Map<String, Object>> drainProviderEvents() {
        return context.getProviderRegistry().getEventBus().drain();
    }

    public List<Map<String, Object>> snapshotProviderEvents() {
        return context.getProviderRegistry().getEventBus().snapshot();
    }

    private void addEvents(List<Map<String, Object>> target, String source, List<Map<String, Object>> events) {
        for (Map<String, Object> event : events) {
            Map<String, Object> tagged = new LinkedHashMap<>(event);
            tagged.put("source", source);
            target.add(tagged);
        }
    }
}
