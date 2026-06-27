package org.takesome.frozenlands.engine.tasks;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventTopics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class EngineTaskPool implements AutoCloseable {
    private static final int DEFAULT_QUEUE_CAPACITY = 256;
    private static final long SHUTDOWN_WAIT_MILLIS = 750L;

    private final EngineContext context;
    private final ThreadPoolExecutor executor;
    private final Map<String, EngineTaskHandle<?>> tasksById = new java.util.concurrent.ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public EngineTaskPool(EngineContext context) {
        this.context = Objects.requireNonNull(context, "context");
        int workers = workerCount();
        int queueCapacity = Math.max(16, Integer.getInteger("frozenlands.taskPool.queueCapacity", DEFAULT_QUEUE_CAPACITY));
        this.executor = new ThreadPoolExecutor(
                workers,
                workers,
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new EngineTaskThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public EngineTaskHandle<Void> execute(String owner, String taskName, Runnable runnable) {
        return execute(owner, taskName, "", runnable);
    }

    public EngineTaskHandle<Void> execute(String owner, String taskName, String description, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return submit(owner, taskName, description, () -> {
            runnable.run();
            return null;
        });
    }

    public <T> EngineTaskHandle<T> submit(String owner, String taskName, Callable<T> callable) {
        return submit(owner, taskName, "", callable);
    }

    public <T> EngineTaskHandle<T> submit(String owner, String taskName, String description, Callable<T> callable) {
        Objects.requireNonNull(callable, "callable");
        EngineTaskHandle<T> handle = new EngineTaskHandle<>(nextId(owner, taskName), safe(owner, "core"), safe(description, ""));
        tasksById.put(handle.id(), handle);
        publish(EngineEventTopics.ENGINE_TASK_SUBMITTED, handle);

        try {
            Future<?> future = executor.submit(() -> runTask(handle, callable));
            handle.attachFuture(future);
        } catch (RejectedExecutionException exception) {
            handle.markRejected(exception);
            publish(EngineEventTopics.ENGINE_TASK_REJECTED, handle);
        }
        return handle;
    }

    public Optional<EngineTaskHandle<?>> find(String id) {
        return Optional.ofNullable(tasksById.get(id));
    }

    public List<Map<String, Object>> snapshot() {
        List<EngineTaskHandle<?>> handles = new ArrayList<>(tasksById.values());
        handles.sort(Comparator.comparing(handle -> String.valueOf(handle.snapshot().get("submittedAt"))));
        List<Map<String, Object>> result = new ArrayList<>(handles.size());
        for (EngineTaskHandle<?> handle : handles) {
            result.add(handle.snapshot());
        }
        return result;
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ok", true);
        status.put("workers", executor.getCorePoolSize());
        status.put("active", executor.getActiveCount());
        status.put("queued", executor.getQueue().size());
        status.put("completed", executor.getCompletedTaskCount());
        status.put("registered", tasksById.size());
        status.put("shutdown", executor.isShutdown());
        return status;
    }

    public boolean cancel(String id, boolean mayInterruptIfRunning) {
        EngineTaskHandle<?> handle = tasksById.get(id);
        if (handle == null) {
            return false;
        }
        boolean cancelled = handle.cancel(mayInterruptIfRunning);
        if (cancelled) {
            publish(EngineEventTopics.ENGINE_TASK_CANCELLED, handle);
        }
        return cancelled;
    }

    @Override
    public void close() {
        executor.shutdownNow();
        for (EngineTaskHandle<?> handle : tasksById.values()) {
            if (!handle.done()) {
                handle.markCancelled();
                publish(EngineEventTopics.ENGINE_TASK_CANCELLED, handle);
            }
        }
        try {
            executor.awaitTermination(SHUTDOWN_WAIT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private <T> void runTask(EngineTaskHandle<T> handle, Callable<T> callable) {
        if (!handle.markRunning()) {
            return;
        }
        publish(EngineEventTopics.ENGINE_TASK_STARTED, handle);
        try {
            T result = callable.call();
            handle.markCompleted(result);
            publish(EngineEventTopics.ENGINE_TASK_COMPLETED, handle);
        } catch (Throwable throwable) {
            if (Thread.currentThread().isInterrupted()) {
                handle.markCancelled();
                publish(EngineEventTopics.ENGINE_TASK_CANCELLED, handle);
                return;
            }
            handle.markFailed(throwable);
            publish(EngineEventTopics.ENGINE_TASK_FAILED, handle);
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(throwable);
        }
    }

    private void publish(String topic, EngineTaskHandle<?> handle) {
        context.getModuleRegistry().publishEvent(topic, handle.snapshot());
    }

    private String nextId(String owner, String taskName) {
        return safe(owner, "core") + ":" + safe(taskName, "task") + ":" + sequence.incrementAndGet();
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private int workerCount() {
        int configured = Integer.getInteger("frozenlands.taskPool.workers", 0);
        if (configured > 0) {
            return configured;
        }
        return Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
    }

    private static final class EngineTaskThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "FrozenLands-Task-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
