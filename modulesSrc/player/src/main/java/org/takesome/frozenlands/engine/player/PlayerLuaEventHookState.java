package org.takesome.frozenlands.engine.player;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.lua.LuaScriptExecutor;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerLuaEventHookState extends BaseAppState {
    private static final String LUA_EVENTS_ROOT = "src/main/resources/scripts/lua";
    private static final String PLAYER_CAMERA_VIEW_SCRIPT = "events/player_camera_view_changed.lua";
    private static final String PLAYER_FOOTSTEP_SCRIPT = "events/player_footstep.lua";
    private static final String PLAYER_SPAWNED_SCRIPT = "events/player_spawned.lua";
    private static final String PLAYER_LANDED_SCRIPT = "events/player_landed.lua";
    private static final String PLAYER_GRINDABLE_HIT_SCRIPT = "events/player_grindable_hit.lua";
    private static final String PLAYER_GRINDABLE_DESTROYED_SCRIPT = "events/player_grindable_destroyed.lua";

    private final EngineContext context;
    private final LuaScriptExecutor executor;
    private final ModuleIndexCatalog moduleIndex = ModuleIndexCatalog.defaultCatalog();
    private final List<AutoCloseable> subscriptions = new ArrayList<>();

    public PlayerLuaEventHookState(EngineContext context) {
        this.context = context;
        this.executor = new LuaScriptExecutor(context);
    }

    @Override
    protected void initialize(Application application) {
        subscribeLuaHook(EngineEventTopics.PLAYER_CAMERA_VIEW_CHANGED, PLAYER_CAMERA_VIEW_SCRIPT);
        subscribeLuaHook(EngineEventTopics.PLAYER_FOOTSTEP, PLAYER_FOOTSTEP_SCRIPT);
        subscribeLuaHook(EngineEventTopics.PLAYER_SPAWNED, PLAYER_SPAWNED_SCRIPT);
        subscribeLuaHook(EngineEventTopics.PLAYER_LANDED, PLAYER_LANDED_SCRIPT);
        subscribeLuaHook(EngineEventTopics.PLAYER_GRINDABLE_HIT, PLAYER_GRINDABLE_HIT_SCRIPT);
        subscribeLuaHook(EngineEventTopics.PLAYER_GRINDABLE_DESTROYED, PLAYER_GRINDABLE_DESTROYED_SCRIPT);
    }

    @Override
    protected void cleanup(Application application) {
        for (AutoCloseable subscription : subscriptions) {
            try {
                subscription.close();
            } catch (Exception ignored) {
            }
        }
        subscriptions.clear();
    }

    @Override protected void onEnable() { }
    @Override protected void onDisable() { }

    private void subscribeLuaHook(String topic, String script) {
        subscriptions.add(context.getModuleRegistry().getEventBus().subscribe(
                topic,
                event -> runLuaHook(topic, script, event),
                false
        ));
        context.getLogger().info("Player Lua event hook installed: topic={} script={}", topic, script);
    }

    private void runLuaHook(String topic, String script, Map<String, Object> event) {
        Path path = scriptPath(script);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("script", script);
        args.put("topic", topic);
        args.put("event", event == null ? Map.of() : event);
        args.put("payload", payload(event));

        Map<String, Object> response = executor.execute(script, path, readSource(path), args);
        if (Boolean.TRUE.equals(response.get("ok"))) {
            context.getLogger().debug("Player Lua event hook executed: topic={} script={}", topic, script);
        } else {
            context.getLogger().error(
                    "Player Lua event hook failed: topic={} script={} error={} message={}",
                    topic,
                    script,
                    response.get("error"),
                    response.get("message")
            );
        }
    }

    private Path scriptPath(String script) {
        return Path.of(moduleIndex.resolvePath(PlayerModule.ID, LUA_EVENTS_ROOT + "/" + script));
    }

    private String readSource(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to read player Lua event hook: " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        return payload instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
