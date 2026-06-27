package org.takesome.frozenlands.engine.lua;

import org.takesome.frozenlands.engine.EngineContext;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LuaScriptExecutor {
    private static final String ENGINE_NAME = LuaAbi.ENGINE_NAME;
    private static final String ENGINE_VERSION = LuaAbi.ENGINE_VERSION;

    private final EngineContext context;
    private final LuaModuleApiCatalog apiCatalog = new LuaModuleApiCatalog();
    private final LuaValueCodec codec = new LuaValueCodec();
    private final LuaProviderBridge bridge;

    public LuaScriptExecutor(EngineContext context) {
        this.context = context;
        this.bridge = new LuaProviderBridge(context);
    }

    public Map<String, Object> manifest() {
        Map<String, Object> manifest = new LinkedHashMap<>(LuaAbi.descriptor(context.getModuleRegistry().snapshot().keySet()));
        manifest.put("attached", true);
        manifest.put("version", ENGINE_VERSION);
        manifest.put("sandboxed", true);
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
                result.put("return", codec.toJava(returns.arg(1)));
            } else if (returns.narg() > 1) {
                List<Object> values = new ArrayList<>();
                for (int i = 1; i <= returns.narg(); i++) {
                    values.add(codec.toJava(returns.arg(i)));
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
        installEngineDescriptor(globals);
        preloadModuleApis(globals);
        return globals;
    }

    private void disableUnsafeGlobals(Globals globals) {
        for (String disabledGlobal : LuaAbi.disabledGlobals()) {
            globals.set(disabledGlobal, LuaValue.NIL);
        }

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
        script.set("args", codec.toLua(arguments));
        globals.set("script", script);
        globals.set("args", codec.toLua(arguments));
    }

    private void installEngineDescriptor(Globals globals) {
        LuaTable engine = new LuaTable();
        engine.set("id", "takesome.frozenlands");
        engine.set("name", "FrozenLands");
        engine.set("abi", codec.toLua(LuaAbi.descriptor(context.getModuleRegistry().snapshot().keySet())));
        engine.set("runtime", codec.toLua(bridge.exportRuntimeManifest()));
        globals.set("engine", engine);
    }

    private void installJavaBridge(Globals globals) {
        LuaTable java = new LuaTable();
        java.set("abi", codec.toLua(LuaAbi.descriptor(context.getModuleRegistry().snapshot().keySet())));
        java.set("callModule", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String moduleId = args.checkjstring(1);
                String commandId = args.checkjstring(2);
                Map<String, Object> commandArgs = codec.mapFromLua(args.arg(3));
                return codec.toLua(context.getModuleRegistry().call(moduleId, commandId, commandArgs));
            }
        });
        java.set("callProvider", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String providerId = args.checkjstring(1);
                String commandId = args.checkjstring(2);
                Map<String, Object> commandArgs = codec.mapFromLua(args.arg(3));
                return codec.toLua(context.getProviderRegistry().call(providerId, commandId, commandArgs));
            }
        });
        java.set("publishModuleEvent", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return codec.toLua(context.getModuleRegistry().publishEvent(args.checkjstring(1), codec.mapFromLua(args.arg(2))));
            }
        });
        java.set("publishProviderEvent", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return codec.toLua(context.getProviderRegistry().publishEvent(args.checkjstring(1), codec.mapFromLua(args.arg(2))));
            }
        });
        java.set("manifest", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return codec.toLua(bridge.exportRuntimeManifest());
            }
        });
        java.set("console", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return codec.toLua(bridge.executeConsole(args.checkjstring(1)));
            }
        });
        java.set("consoleHelp", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String command = args.narg() >= 1 ? args.arg(1).tojstring() : "";
                return codec.toLua(bridge.exportConsoleHelp(command));
            }
        });
        java.set("consoleVersion", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return codec.toLua(bridge.exportConsoleVersion());
            }
        });
        java.set("commandsList", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return codec.toLua(bridge.exportConsoleCommands());
            }
        });
        java.set("consoleComplete", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String prefix = args.narg() >= 1 ? args.arg(1).tojstring() : "";
                return codec.toLua(bridge.exportConsoleComplete(prefix));
            }
        });
        java.set("eventsSnapshot", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return codec.toLua(bridge.snapshotJavaEvents());
            }
        });
        java.set("eventsDrain", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return codec.toLua(bridge.drainJavaEvents());
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
}
