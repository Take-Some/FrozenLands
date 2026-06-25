package dev.takesome.helix.ui.component.event;

/** Receives canonical retained UI component events. */
@FunctionalInterface
public interface UiComponentEventSink {
    UiComponentEventSink NONE = event -> {};

    void emit(UiComponentEvent event);

    default void emit(UiComponentEvent.Builder builder) {
        if (builder != null) emit(builder.build());
    }
}
