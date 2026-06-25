package org.takesome.frozenlands.engine.lua;

import org.takesome.frozenlands.engine.EngineContext;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LuaScriptExecutor {
    private static final String ENGINE_NAME = "luaj-jse";
    private static final String ENGINE_VERSION = "3.0.1";

    private final EngineContext context;
    private final LuaModuleApiCatalog apiCatalog = new LuaModuleApiCatalog();

    public LuaScriptExecutor(EngineContext context) {
        this.context = context;
    }

    public Map<String, Object> manifest() {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("attached", true);
        manifest.put("engine", ENGINE_NAME);
        manifest.put("version", ENGINE_VERSION);
        manifest.put("bridge", "engine.core/java.callModule/java.callProvider/java.publishModuleEvent");
        manifest.put("sandboxed", true);
        manifest.put("disabledLibs", List.of("io", "os", "debug", "luajava", "dofile", "loadfile"));
        manifest.put("preloadedModules", List.copyOf(context.getModuleRegistry().snapshot().keySet()));
        return manifest;
    }

    public Map<String, Object> execute(String scriptName, Path scriptPath, String source, Map<String, Object> arguments) {
        long startedAt = System.nanoTime();
        Map<String, Object> safeArguments = arguments == null ? Map.of() : new LinkedHashMap<>(arguments);
        Globals globals = createGlobals(scriptName, scriptPath, safeArguments);

        try {
            Varargs returns = globals.load(source, "@" + scriptPath).invoke();
            Map<String, Object> result = baseResult(scriptName, scriptPath, startedAt);
            result.put("ok", true);
            result.put("executed", true);
            result.put("returnCount", returns.narg());
            if (returns.narg() == 1) {
                result.put("return", toJava(returns.arg(1)));
            } else if (returns.narg() > 1) {
                List<Object> values = new ArrayList<>();
                for (int i = 1; i <= returns.narg(); i++) {
                    values.add(toJava(returns.arg(i)));
                }
                result.put("returns", values);
            }
            return result;
        } catch (LuaError e) {
            Map<String, Object> result = baseResult(scriptName, scriptPath, startedAt);
            result.put("ok", false);
            result.put("executed", false);
            result.put("error", "lua-error");
            result.put("message", e.getMessage());
            return result;
        } catch (RuntimeException e) {
            Map<String, Object> result = baseResult(scriptName, scriptPath, startedAt);
            result.put("ok", false);
            result.put("executed", false);
            result.put("error", "java-bridge-error");
            result.put("message", e.getMessage());
            return result;
        }
    }

    private Globals createGlobals(String scriptName, Path scriptPath, Map<String, Object> arguments) {
        Globals globals = new Globals();
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new CoroutineLib());
        globals.load(new JseMathLib());
        LoadState.install(globals);
        LuaC.install(globals);

        disableUnsafeGlobals(globals);
        installPrint(globals);
        installScriptMetadata(globals, scriptName, scriptPath, arguments);
        installJavaBridge(globals);
        preloadModuleApis(globals);
        return globals;
    }

    private void disableUnsafeGlobals(Globals globals) {
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);
        globals.set("luajava", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);

        LuaValue packageValue = globals.get("package");
        if (packageValue.istable()) {
            packageValue.set("path", "");
            packageValue.set("cpath", "");
        }
    }

    private void installPrint(Globals globals) {
        globals.set("print", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                StringBuilder message = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    if (i > 1) {
                        message.append('\t');
                    }
                    message.append(args.arg(i).tojstring());
                }
                context.getLogger().info("[Lua] {}", message);
                return LuaValue.NIL;
            }
        });
    }

    private void installScriptMetadata(Globals globals, String scriptName, Path scriptPath, Map<String, Object> arguments) {
        LuaTable script = new LuaTable();
        script.set("name", scriptName);
        script.set("path", scriptPath.toString());
        script.set("args", toLua(arguments));
        globals.set("script", script);
        globals.set("args", toLua(arguments));
    }

    private void installJavaBridge(Globals globals) {
        LuaTable java = new LuaTable();
        java.set("callModule", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String moduleId = args.checkjstring(1);
                String commandId = args.checkjstring(2);
                Map<String, Object> commandArgs = mapFromLua(args.arg(3));
                return toLua(context.getModuleRegistry().call(moduleId, commandId, commandArgs));
            }
        });
        java.set("callProvider", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String providerId = args.checkjstring(1);
                String commandId = args.checkjstring(2);
                Map<String, Object> commandArgs = mapFromLua(args.arg(3));
                return toLua(context.getProviderRegistry().call(providerId, commandId, commandArgs));
            }
        });
        java.set("publishModuleEvent", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return toLua(context.getModuleRegistry().publishEvent(args.checkjstring(1), mapFromLua(args.arg(2))));
            }
        });
        java.set("publishProviderEvent", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return toLua(context.getProviderRegistry().publishEvent(args.checkjstring(1), mapFromLua(args.arg(2))));
            }
        });
        java.set("manifest", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return toLua(new LuaProviderBridge(context).exportRuntimeManifest());
            }
        });
        java.set("eventsSnapshot", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return toLua(new LuaProviderBridge(context).snapshotJavaEvents());
            }
        });
        java.set("eventsDrain", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return toLua(new LuaProviderBridge(context).drainJavaEvents());
            }
        });
        java.set("emit", java.get("publishModuleEvent"));
        java.set("publishEvent", java.get("publishModuleEvent"));
        globals.set("java", java);
    }

    private void preloadModuleApis(Globals globals) {
        LuaValue packageValue = globals.get("package");
        if (!packageValue.istable()) {
            return;
        }
        LuaValue preloadValue = packageValue.get("preload");
        if (!preloadValue.istable()) {
            return;
        }
        LuaTable preload = (LuaTable) preloadValue;
        for (String moduleId : context.getModuleRegistry().snapshot().keySet()) {
            preload.set(moduleId, new ModuleApiLoader(globals, moduleId));
        }
    }

    private final class ModuleApiLoader extends OneArgFunction {
        private final Globals globals;
        private final String moduleId;

        private ModuleApiLoader(Globals globals, String moduleId) {
            this.globals = globals;
            this.moduleId = moduleId;
        }

        @Override
        public LuaValue call(LuaValue arg) {
            try {
                return globals.load(apiCatalog.readApi(moduleId), "@" + apiCatalog.pathFor(moduleId)).call();
            } catch (RuntimeException e) {
                throw new LuaError("Failed to require Lua module API: " + moduleId + ": " + e.getMessage());
            }
        }
    }

    private Map<String, Object> baseResult(String scriptName, Path scriptPath, long startedAt) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("script", scriptName);
        result.put("path", scriptPath.toString());
        result.put("executor", ENGINE_NAME);
        result.put("durationMs", (System.nanoTime() - startedAt) / 1_000_000.0d);
        return result;
    }

    private Map<String, Object> mapFromLua(LuaValue value) {
        if (value == null || value.isnil()) {
            return Map.of();
        }
        Object converted = toJava(value);
        if (converted instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, val) -> result.put(String.valueOf(key), val));
            return result;
        }
        return Map.of();
    }

    private Object toJava(LuaValue value) {
        if (value == null || value.isnil()) {
            return null;
        }
        if (value.isboolean()) {
            return value.toboolean();
        }
        if (value.isinttype()) {
            return value.toint();
        }
        if (value.isnumber()) {
            return value.todouble();
        }
        if (value.isstring()) {
            return value.tojstring();
        }
        if (value.istable()) {
            return tableToJava((LuaTable) value);
        }
        return value.tojstring();
    }

    private Object tableToJava(LuaTable table) {
        Map<Object, Object> entries = new LinkedHashMap<>();
        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs next = table.next(key);
            key = next.arg1();
            if (key.isnil()) {
                break;
            }
            entries.put(toJavaKey(key), toJava(next.arg(2)));
        }
        if (isArrayLike(entries)) {
            List<Object> list = new ArrayList<>();
            for (int i = 1; i <= entries.size(); i++) {
                list.add(entries.get(i));
            }
            return list;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        entries.forEach((entryKey, entryValue) -> map.put(String.valueOf(entryKey), entryValue));
        return map;
    }

    private Object toJavaKey(LuaValue key) {
        if (key instanceof LuaInteger || key.isinttype()) {
            return key.toint();
        }
        if (key instanceof LuaString || key.isstring()) {
            return key.tojstring();
        }
        if (key.isnumber()) {
            return key.todouble();
        }
        return key.tojstring();
    }

    private boolean isArrayLike(Map<Object, Object> entries) {
        if (entries.isEmpty()) {
            return false;
        }
        for (Object key : entries.keySet()) {
            if (!(key instanceof Integer index) || index < 1 || index > entries.size()) {
                return false;
            }
        }
        for (int i = 1; i <= entries.size(); i++) {
            if (!entries.containsKey(i)) {
                return false;
            }
        }
        return true;
    }

    private LuaValue toLua(Object value) {
        if (value == null) {
            return LuaValue.NIL;
        }
        if (value instanceof LuaValue luaValue) {
            return luaValue;
        }
        if (value instanceof Boolean bool) {
            return LuaValue.valueOf(bool);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return LuaValue.valueOf(((Number) value).longValue());
        }
        if (value instanceof Number number) {
            return LuaValue.valueOf(number.doubleValue());
        }
        if (value instanceof CharSequence chars) {
            return LuaValue.valueOf(chars.toString());
        }
        if (value instanceof Map<?, ?> map) {
            LuaTable table = new LuaTable();
            map.forEach((key, val) -> table.set(String.valueOf(key), toLua(val)));
            return table;
        }
        if (value instanceof Iterable<?> iterable) {
            LuaTable table = new LuaTable();
            int index = 1;
            for (Object item : iterable) {
                table.set(index++, toLua(item));
            }
            return table;
        }
        if (value.getClass().isArray()) {
            LuaTable table = new LuaTable();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                table.set(i + 1, toLua(Array.get(value, i)));
            }
            return table;
        }
        return LuaValue.valueOf(String.valueOf(value));
    }
}
