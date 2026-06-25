package dev.takesome.helix.ui.runtime;

import dev.takesome.helix.concurrent.EngineQueues;
import dev.takesome.helix.concurrent.EngineTaskPools;
import dev.takesome.helix.concurrent.TaskExecutor;
import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Bounded worker pool for CPU-safe UI preparation work.
 *
 * <p>This pool is deliberately not a render-thread replacement. LibGDX/OpenGL,
 * texture creation, SpriteBatch, BitmapFont drawing and retained-node mutation
 * must stay on the UI/render thread. Use this pool for parsing, validation,
 * metadata warm-up and other non-GL preparation. UI-thread callbacks are drained
 * by {@link EngineUiRuntime#update(float)} with a per-frame budget.</p>
 */
public final class EngineUiThreadPool implements AutoCloseable {
    private static final Logger LOG = EngineLog.logger(EngineUiThreadPool.class);
    private static final int DEFAULT_THREADS = Math.max(1, Math.min(2, EngineTaskPools.defaultCpuThreads()));
    private static final int DEFAULT_CALLBACK_BUDGET = 16;

    private final TaskExecutor executor;
    private final Queue<Runnable> uiThreadCallbacks = EngineQueues.mpscUnbounded("engine-ui.callbacks");
    private final AtomicInteger pending = new AtomicInteger();
    private final int callbackBudget;

    private EngineUiThreadPool(TaskExecutor executor, int callbackBudget) {
        this.executor = executor;
        this.callbackBudget = Math.max(1, callbackBudget);
    }

    public static EngineUiThreadPool createDefault() {
        int threads = intProperty("helix.ui.threadPool.threads", DEFAULT_THREADS);
        int safeThreads = Math.max(1, Math.min(8, threads));
        int callbackBudget = intProperty("helix.ui.threadPool.callbackBudget", DEFAULT_CALLBACK_BUDGET);
        return new EngineUiThreadPool(EngineTaskPools.fixed("engine-ui", safeThreads), callbackBudget);
    }

    public int threads() {
        return executor.threads();
    }

    public int pendingTasks() {
        return pending.get();
    }

    public int queuedCallbacks() {
        return uiThreadCallbacks.size();
    }

    public int callbackBudget() {
        return callbackBudget;
    }

    public CompletableFuture<Void> run(String name, Runnable task) {
        if (task == null) return CompletableFuture.completedFuture(null);
        return supply(name, () -> {
            task.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> supply(String name, Callable<T> task) {
        if (task == null) {
            CompletableFuture<T> result = new CompletableFuture<>();
            result.completeExceptionally(new IllegalArgumentException("UI worker task must not be null"));
            return result;
        }
        pending.incrementAndGet();
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            executor.supply(task).whenComplete((value, error) -> {
                pending.decrementAndGet();
                if (error != null) result.completeExceptionally(error);
                else result.complete(value);
            });
        } catch (RejectedExecutionException ex) {
            pending.decrementAndGet();
            LOG.warn("Engine UI worker rejected task='{}' pending={}", safeName(name), pending.get(), ex);
            result.completeExceptionally(ex);
        } catch (RuntimeException ex) {
            pending.decrementAndGet();
            result.completeExceptionally(ex);
        }
        return result;
    }

    public <T> CompletableFuture<T> supplyThenOnUiThread(
            String name,
            Callable<T> task,
            Consumer<T> onSuccess,
            Consumer<Throwable> onFailure
    ) {
        CompletableFuture<T> result = new CompletableFuture<>();
        supply(name, task).whenComplete((value, error) -> uiThreadCallbacks.offer(() -> {
            try {
                if (error != null) {
                    if (onFailure != null) onFailure.accept(error);
                    result.completeExceptionally(error);
                } else {
                    if (onSuccess != null) onSuccess.accept(value);
                    result.complete(value);
                }
            } catch (RuntimeException callbackError) {
                LOG.warn("Engine UI worker callback failed task='{}'", safeName(name), callbackError);
                result.completeExceptionally(callbackError);
            }
        }));
        return result;
    }

    int drainUiThreadCallbacks() {
        return drainUiThreadCallbacks(callbackBudget);
    }

    int drainUiThreadCallbacks(int maxCallbacks) {
        int limit = Math.max(0, maxCallbacks);
        int processed = 0;
        while (processed < limit) {
            Runnable callback = uiThreadCallbacks.poll();
            if (callback == null) break;
            try {
                callback.run();
            } catch (RuntimeException ex) {
                LOG.warn("Engine UI worker UI-thread callback failed", ex);
            }
            processed++;
        }
        return processed;
    }

    public List<Runnable> shutdownNow() {
        uiThreadCallbacks.clear();
        return executor.shutdownNow();
    }

    public void shutdown() {
        uiThreadCallbacks.clear();
        executor.shutdown();
    }

    @Override
    public void close() {
        shutdown();
    }

    private static int intProperty(String key, int fallback) {
        String value = System.getProperty(key, "");
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String safeName(String name) {
        return name == null || name.isBlank() ? "unnamed" : name.trim();
    }
}
