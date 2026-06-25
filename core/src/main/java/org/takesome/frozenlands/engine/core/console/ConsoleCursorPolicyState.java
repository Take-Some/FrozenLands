package org.takesome.frozenlands.engine.core.console;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventTopics;

import java.util.Map;

public final class ConsoleCursorPolicyState extends BaseAppState {
    private final EngineContext context;
    private AutoCloseable subscription;

    public ConsoleCursorPolicyState(EngineContext context) {
        this.context = context;
    }

    @Override
    protected void initialize(Application application) {
        subscription = context.getModuleRegistry().getEventBus().subscribe(
                EngineEventTopics.CURSOR_VISIBILITY_REQUESTED,
                this::onCursorVisibilityRequested,
                true
        );
    }

    @Override
    protected void cleanup(Application application) {
        if (subscription != null) {
            try {
                subscription.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override protected void onEnable() { }
    @Override protected void onDisable() { }

    private void onCursorVisibilityRequested(Map<String, Object> event) {
        context.getInputManager().setCursorVisible(booleanPayload(event, "visible"));
    }

    private boolean booleanPayload(Map<String, Object> event, String key) {
        Object payload = event == null ? null : event.get("payload");
        return payload instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get(key));
    }
}
