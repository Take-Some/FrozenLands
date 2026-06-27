package org.takesome.frozenlands.engine.tasks;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public final class EngineTaskHandle<T> {
    private final String id;
    private final String owner;
    private final String description;
    private final Instant submittedAt;
    private final AtomicReference<EngineTaskStatus> status = new AtomicReference<>(EngineTaskStatus.QUEUED);

    private volatile Future<?> future;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile T result;
    private volatile Throwable error;

    EngineTaskHandle(String id, String owner, String description) {
        this.id = id;
        this.owner = owner;
        this.description = description;
        this.submittedAt = Instant.now();
    }

    public String id() { return id; }
    public String owner() { return owner; }
    public String description() { return description; }
    public EngineTaskStatus status() { return status.get(); }
    public T resultOrNull() { return result; }
    public Throwable errorOrNull() { return error; }

    public boolean done() {
        EngineTaskStatus current = status();
        return current == EngineTaskStatus.COMPLETED
                || current == EngineTaskStatus.FAILED
                || current == EngineTaskStatus.CANCELLED
                || current == EngineTaskStatus.REJECTED;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        Future<?> currentFuture = future;
        boolean futureCancelled = currentFuture != null && currentFuture.cancel(mayInterruptIfRunning);
        if (futureCancelled || status.compareAndSet(EngineTaskStatus.QUEUED, EngineTaskStatus.CANCELLED)) {
            markCancelled();
            return true;
        }
        return false;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", id);
        snapshot.put("owner", owner);
        snapshot.put("description", description);
        snapshot.put("status", status().name());
        snapshot.put("submittedAt", submittedAt.toString());
        snapshot.put("startedAt", startedAt == null ? null : startedAt.toString());
        snapshot.put("finishedAt", finishedAt == null ? null : finishedAt.toString());
        snapshot.put("elapsedMillis", elapsedMillis());
        if (error != null) {
            snapshot.put("error", error.getClass().getSimpleName());
            snapshot.put("message", error.getMessage());
        }
        return snapshot;
    }

    void attachFuture(Future<?> future) {
        this.future = future;
    }

    boolean markRunning() {
        boolean changed = status.compareAndSet(EngineTaskStatus.QUEUED, EngineTaskStatus.RUNNING);
        if (changed) {
            startedAt = Instant.now();
        }
        return changed;
    }

    void markCompleted(T result) {
        this.result = result;
        finishedAt = Instant.now();
        status.set(EngineTaskStatus.COMPLETED);
    }

    void markFailed(Throwable error) {
        this.error = error;
        finishedAt = Instant.now();
        status.set(EngineTaskStatus.FAILED);
    }

    void markCancelled() {
        finishedAt = Instant.now();
        status.set(EngineTaskStatus.CANCELLED);
    }

    void markRejected(Throwable error) {
        this.error = error;
        finishedAt = Instant.now();
        status.set(EngineTaskStatus.REJECTED);
    }

    private long elapsedMillis() {
        Instant end = finishedAt == null ? Instant.now() : finishedAt;
        Instant start = startedAt == null ? submittedAt : startedAt;
        return Duration.between(start, end).toMillis();
    }
}
