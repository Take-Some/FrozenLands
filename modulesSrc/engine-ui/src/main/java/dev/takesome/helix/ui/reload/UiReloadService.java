package dev.takesome.helix.ui.reload;

import dev.takesome.helix.io.ResourcePath;
import dev.takesome.helix.io.watch.Reloadable;
import dev.takesome.helix.io.watch.adapter.UiReloadable;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class UiReloadService implements Reloadable {
    private final CopyOnWriteArrayList<UiReloadTask> tasks = new CopyOnWriteArrayList<>();

    public UiReloadService register(UiReloadTask task) {
        tasks.add(task);
        tasks.sort(Comparator.comparingInt(UiReloadTask::order).thenComparing(UiReloadTask::id));
        return this;
    }

    public List<UiReloadTask> tasks() { return List.copyOf(tasks); }
    @Override public void reload(ResourcePath path) { reloadAll(path); }

    public int reloadAll(ResourcePath path) {
        int count = 0;
        for (UiReloadTask task : tasks) {
            if (task.supports(path)) { task.reload(path); count++; }
        }
        return count;
    }

    public UiReloadable adapter() { return new UiReloadable(change -> reload(change.path())); }
}
