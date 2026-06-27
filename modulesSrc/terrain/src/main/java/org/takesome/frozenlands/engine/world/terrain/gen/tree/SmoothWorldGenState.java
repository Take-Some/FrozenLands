package org.takesome.frozenlands.engine.world.terrain.gen.tree;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import org.takesome.frozenlands.engine.EngineContext;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class SmoothWorldGenState extends BaseAppState {
    private static final int DEFAULT_MAX_OPERATIONS_PER_FRAME = 4;
    private static final long DEFAULT_FRAME_BUDGET_NANOS = 2_000_000L;

    private final EngineContext context;
    private final Queue<QueuedWorldGenOperation> operations = new ConcurrentLinkedQueue<>();
    private final int maxOperationsPerFrame;
    private final long frameBudgetNanos;

    public SmoothWorldGenState(EngineContext context) {
        this.context = context;
        this.maxOperationsPerFrame = Math.max(1, Integer.getInteger(
                "frozenlands.smoothWorldGen.maxOperationsPerFrame",
                DEFAULT_MAX_OPERATIONS_PER_FRAME
        ));
        this.frameBudgetNanos = Math.max(250_000L, Long.getLong(
                "frozenlands.smoothWorldGen.frameBudgetNanos",
                DEFAULT_FRAME_BUDGET_NANOS
        ));
    }

    public void enqueue(String label, Runnable operation) {
        if (operation == null) {
            return;
        }
        operations.add(new QueuedWorldGenOperation(label == null ? "worldgen" : label, operation));
    }

    public int pendingOperations() {
        return operations.size();
    }

    @Override
    public void update(float tpf) {
        long started = System.nanoTime();
        int processed = 0;

        while (processed < maxOperationsPerFrame && System.nanoTime() - started < frameBudgetNanos) {
            QueuedWorldGenOperation operation = operations.poll();
            if (operation == null) {
                return;
            }
            try {
                operation.run();
            } catch (RuntimeException exception) {
                context.getLogger().warn("SmoothWorldGen operation failed: {}", operation.label(), exception);
            }
            processed++;
        }
    }

    @Override
    protected void initialize(Application application) {
    }

    @Override
    protected void cleanup(Application application) {
        operations.clear();
    }

    @Override protected void onEnable() { }
    @Override protected void onDisable() { }

    private record QueuedWorldGenOperation(String label, Runnable runnable) {
        void run() {
            runnable.run();
        }
    }
}
