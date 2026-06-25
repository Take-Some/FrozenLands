package org.takesome.frozenlands.engine.core;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventTopics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LuaEventHookState extends BaseAppState {
    private static final String PLAYER_CAMERA_VIEW_SCRIPT = "events/player_camera_view_changed.lua";

    private final EngineContext context;
    private final ScriptRuntime scripts;
    private final List<AutoCloseable> subscriptions = new ArrayList<>();

    public LuaEventHookState(EngineContext context) {
        this.context = context;
        this.scripts = new ScriptRuntime(context);
    }

    @Override
    protected void initialize(Application application) {
        subscribeLuaHook(EngineEventTopics.PLAYER_CAMERA_VIEW_CHANGED, PLAYER_CAMERA_VIEW_SCRIPT);
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
        context.getLogger().info("Lua event hook installed: topic={} script={}", topic, script);
    }

    private void runLuaHook(String topic, String script, Map<String, Object> event) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("script", script);
        args.put("topic", topic);
        args.put("event", event == null ? Map.of() : event);
        args.put("payload", payload(event));

        Map<String, Object> response = scripts.run(args);
        if (Boolean.TRUE.equals(response.get("ok"))) {
            context.getLogger().debug("Lua event hook executed: topic={} script={}", topic, script);
        } else {
            context.getLogger().error(
                    "Lua event hook failed: topic={} script={} error={} message={}",
                    topic,
                    script,
                    response.get("error"),
                    response.get("message")
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        return payload instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
