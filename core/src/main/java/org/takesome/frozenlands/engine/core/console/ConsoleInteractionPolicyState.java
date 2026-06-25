package org.takesome.frozenlands.engine.core.console;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventTopics;

import java.util.Map;

public final class ConsoleInteractionPolicyState extends BaseAppState {
    private final EngineContext context;
    private AutoCloseable subscription;

    public ConsoleInteractionPolicyState(EngineContext context) {
        this.context = context;
    }

    @Override
    protected void initialize(Application application) {
        subscription = context.getModuleRegistry().getEventBus().subscribe(
                EngineEventTopics.CONSOLE_VISIBILITY_CHANGED,
                this::onConsoleVisibilityChanged,
                true
        );
        publishInteractionState(false);
    }

    @Override
    protected void cleanup(Application application) {
        close(subscription);
    }

    @Override protected void onEnable() { }
    @Override protected void onDisable() { }

    private void onConsoleVisibilityChanged(Map<String, Object> event) {
        publishInteractionState(booleanPayload(event, "open"));
    }

    private void publishInteractionState(boolean open) {
        context.getModuleRegistry().publishEvent(EngineEventTopics.CURSOR_VISIBILITY_REQUESTED, Map.of("visible", open));
        context.getModuleRegistry().publishEvent(EngineEventTopics.CAMERA_FOLLOW_PAUSE_REQUESTED, Map.of("paused", open));
        context.getModuleRegistry().publishEvent(EngineEventTopics.CAMERA_LOOK_INPUT_ENABLED_REQUESTED, Map.of("enabled", !open));
    }

    private boolean booleanPayload(Map<String, Object> event, String key) {
        Object payload = event == null ? null : event.get("payload");
        return payload instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get(key));
    }

    private void close(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
