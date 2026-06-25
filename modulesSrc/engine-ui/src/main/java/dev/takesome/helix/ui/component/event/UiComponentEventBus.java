package dev.takesome.helix.ui.component.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Small in-process fan-out bus for retained UI component events. */
public final class UiComponentEventBus implements UiComponentEventSink {
    private final CopyOnWriteArrayList<UiComponentEventSink> sinks = new CopyOnWriteArrayList<>();

    public AutoCloseable subscribe(UiComponentEventSink sink) {
        if (sink == null || sink == UiComponentEventSink.NONE) return () -> {};
        sinks.add(sink);
        return () -> sinks.remove(sink);
    }

    public void clear() { sinks.clear(); }
    public List<UiComponentEventSink> sinks() { return List.copyOf(sinks); }

    @Override
    public void emit(UiComponentEvent event) {
        if (event == null) return;
        for (UiComponentEventSink sink : sinks) sink.emit(event);
    }
}
