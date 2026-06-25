package org.takesome.frozenlands.engine.core;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;
import org.takesome.frozenlands.engine.lua.LuaScriptExecutor;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

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

    public ScriptRuntime(EngineContext context) {
        this.executor = new LuaScriptExecutor(context);
    }

    public Map<String, Object> manifest() {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("enabled", runtimeConfig.bool(scripting, "enabled", true));
        manifest.put("roots", scriptRoots().stream().map(Path::toString).toList());
        manifest.put("scripts", listScripts());
        manifest.put("entrypoints", listValue(scripting.get("entrypoints")));
        manifest.put("autoRun", autoRunScripts());
        manifest.put("executor", executor.manifest());
        return manifest;
    }

    public Map<String, Object> list() {
        return result("scripts", listScripts());
    }

    public Map<String, Object> read(Map<String, Object> arguments) {
        String script = stringArg(arguments, "script", defaultEntrypoint());
        Path path = locateScript(script);
        try {
            Map<String, Object> response = result("script", script);
            response.put("path", path.toString());
            response.put("source", Files.readString(path, StandardCharsets.UTF_8));
            return response;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read script: " + path, e);
        }
    }

    public Map<String, Object> run(Map<String, Object> arguments) {
        if (!runtimeConfig.bool(scripting, "enabled", true)) {
            Map<String, Object> response = result("script", stringArg(arguments, "script", defaultEntrypoint()));
            response.put("ok", false);
            response.put("executed", false);
            response.put("reason", "scripting-disabled");
            return response;
        }

        String script = stringArg(arguments, "script", defaultEntrypoint());
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
        return listValue(scripting.get("autoRun"));
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
        for (Path root : scriptRoots()) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".lua"))
                        .forEach(path -> scripts.add(root.relativize(path).toString().replace('\\', '/')));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to scan script root: " + root, e);
            }
        }
        return scripts;
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
        List<Object> roots = listValue(scripting.get("scriptRoots"));
        if (roots.isEmpty()) {
            roots = List.of("assets/scripts");
        }
        return roots.stream()
                .map(String::valueOf)
                .map(root -> Path.of(moduleIndex.resolvePath(MODULE_ID, root)))
                .toList();
    }

    private String defaultEntrypoint() {
        List<Object> entrypoints = listValue(scripting.get("entrypoints"));
        return entrypoints.isEmpty() ? "startup.lua" : String.valueOf(entrypoints.get(0));
    }

    private Map<String, Object> scriptArguments(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>(arguments);
        copy.remove("script");
        return copy;
    }

    private List<Object> listValue(Object value) {
        return value instanceof List<?> list ? new ArrayList<>(list) : List.of();
    }

    private String stringArg(Map<String, Object> arguments, String key, String fallback) {
        Object value = arguments == null ? null : arguments.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private Map<String, Object> result(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put(key, value);
        return result;
    }
}
