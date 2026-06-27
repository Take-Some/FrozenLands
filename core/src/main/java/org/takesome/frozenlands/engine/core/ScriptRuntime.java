package org.takesome.frozenlands.engine.core;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;
import org.takesome.frozenlands.engine.lua.LuaScriptExecutor;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;
import org.takesome.frozenlands.engine.runtime.RuntimeMaps;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScriptRuntime {
    private static final String MODULE_ID = CoreModule.ID;

    private final ModuleIndexCatalog moduleIndex = ModuleIndexCatalog.defaultCatalog();
    private final LuaRuntimeConfig runtimeConfig = new LuaRuntimeConfig();
    private final LuaScriptExecutor executor;
    private final Map<String, Object> config = runtimeConfig.read(MODULE_ID);
    private final Map<String, Object> scripting = runtimeConfig.map(config, "scripting");
    private final Map<String, Object> scriptingDefaults = runtimeConfig.map(scripting, "defaults");

    public ScriptRuntime(EngineContext context) {
        this.executor = new LuaScriptExecutor(context);
    }

    public Map<String, Object> manifest() {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("enabled", runtimeConfig.bool(scripting, "enabled", true));
        manifest.put("roots", scriptRoots().stream().map(Path::toString).toList());
        manifest.put("scripts", listScripts());
        manifest.put("entrypoints", RuntimeMaps.list(scripting.get("entrypoints")));
        manifest.put("autoRun", autoRunScripts());
        manifest.put("scriptExtensions", scriptExtensions());
        manifest.put("defaults", scriptingDefaults);
        manifest.put("executor", executor.manifest());
        return manifest;
    }

    public Map<String, Object> list() {
        return RuntimeMaps.result("scripts", listScripts());
    }

    public Map<String, Object> read(Map<String, Object> arguments) {
        String script = RuntimeMaps.string(arguments, "script", defaultEntrypoint());
        Path path = locateScript(script);
        try {
            Map<String, Object> response = RuntimeMaps.result("script", script);
            response.put("path", path.toString());
            response.put("source", Files.readString(path, StandardCharsets.UTF_8));
            return response;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read script: " + path, e);
        }
    }

    public Map<String, Object> run(Map<String, Object> arguments) {
        if (!runtimeConfig.bool(scripting, "enabled", true)) {
            Map<String, Object> response = RuntimeMaps.result(
                    "script",
                    RuntimeMaps.string(arguments, "script", defaultEntrypoint())
            );
            response.put("ok", false);
            response.put("executed", false);
            response.put("reason", "scripting-disabled");
            return response;
        }

        String script = RuntimeMaps.string(arguments, "script", defaultEntrypoint());
        Path path = locateScript(script);
        String source = readSource(path);
        Map<String, Object> response = executor.execute(script, path, source, scriptArguments(arguments));
        response.put("args", scriptArguments(arguments));
        return response;
    }

    public List<Map<String, Object>> runAutoRunScripts() {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Object configured : autoRunScripts()) {
            String script = String.valueOf(configured);
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("script", script);
            args.put("autoRun", true);
            results.add(run(args));
        }
        return results;
    }

    public List<Object> autoRunScripts() {
        return RuntimeMaps.list(scripting.get("autoRun"));
    }

    private String readSource(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read script: " + path, e);
        }
    }

    private List<String> listScripts() {
        List<String> scripts = new ArrayList<>();
        List<String> extensions = scriptExtensions();
        for (Path root : scriptRoots()) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> matchesScriptExtension(path.getFileName().toString(), extensions))
                        .forEach(path -> scripts.add(root.relativize(path).toString().replace('\\', '/')));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to scan script root: " + root, e);
            }
        }
        return scripts;
    }

    private boolean matchesScriptExtension(String fileName, List<String> extensions) {
        for (String extension : extensions) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private Path locateScript(String script) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("Script name must not be blank");
        }
        Path requested = Path.of(script);
        if (requested.isAbsolute() || script.contains("..")) {
            throw new IllegalArgumentException("Script path must be relative to an indexed script root: " + script);
        }
        for (Path root : scriptRoots()) {
            Path candidate = root.resolve(requested).normalize();
            if (candidate.startsWith(root) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Script is not found in indexed script roots: " + script);
    }

    private List<Path> scriptRoots() {
        List<Object> roots = RuntimeMaps.list(scripting.get("scriptRoots"));
        if (roots.isEmpty()) {
            roots = List.of(runtimeConfig.string(scriptingDefaults, "scriptRoot", "assets/scripts"));
        }
        return roots.stream()
                .map(String::valueOf)
                .map(root -> Path.of(moduleIndex.resolvePath(MODULE_ID, root)))
                .toList();
    }

    private List<String> scriptExtensions() {
        List<Object> configured = RuntimeMaps.list(scripting.get("scriptExtensions"));
        if (configured.isEmpty()) {
            return List.of(".lua");
        }
        return configured.stream().map(String::valueOf).toList();
    }

    private String defaultEntrypoint() {
        List<Object> entrypoints = RuntimeMaps.list(scripting.get("entrypoints"));
        String fallback = runtimeConfig.string(scriptingDefaults, "entrypoint", "startup.lua");
        return entrypoints.isEmpty() ? fallback : String.valueOf(entrypoints.get(0));
    }

    private Map<String, Object> scriptArguments(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("script");
        return copy;
    }
}
