package dev.takesome.helix.ui.runtime;

import dev.takesome.helix.ui.scene.SceneManager;
import dev.takesome.helix.update.EngineUpdatable;
import dev.takesome.helix.update.EngineUpdateFrame;

import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Engine-owned update driver for retained UI scene managers.
 *
 * <p>Node.update(dt) should be reached through this module runtime when HELIX is
 * launched through engine-runtime.</p>
 */
public final class EngineUiRuntime implements EngineUpdatable {
    private static final EngineUiRuntime CURRENT = new EngineUiRuntime();

    private final CopyOnWriteArraySet<SceneManager> sceneManagers = new CopyOnWriteArraySet<>();
    private final EngineUiThreadPool threadPool = EngineUiThreadPool.createDefault();
    private volatile float lastDeltaSeconds = 1f / 60f;

    private EngineUiRuntime() {
    }

    public static EngineUiRuntime current() {
        return CURRENT;
    }

    @Override
    public String id() {
        return "engine.ui.runtime";
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public void update(EngineUpdateFrame frame) {
        update(frame == null ? 0f : frame.deltaSeconds());
    }

    public void update(float dt) {
        float step = Float.isFinite(dt) && dt > 0f ? Math.min(dt, 0.25f) : 0f;
        lastDeltaSeconds = step;
        threadPool.drainUiThreadCallbacks();
        for (SceneManager sceneManager : sceneManagers) {
            sceneManager.update(step);
        }
        threadPool.drainUiThreadCallbacks();
    }

    public void register(SceneManager sceneManager) {
        if (sceneManager != null) sceneManagers.add(sceneManager);
    }

    public void unregister(SceneManager sceneManager) {
        if (sceneManager != null) sceneManagers.remove(sceneManager);
    }

    public List<SceneManager> sceneManagers() {
        return List.copyOf(sceneManagers);
    }

    public EngineUiThreadPool threadPool() {
        return threadPool;
    }

    public float lastDeltaSeconds() { return lastDeltaSeconds; }

    public void shutdown() {
        threadPool.shutdown();
        sceneManagers.clear();
    }

}
