package dev.takesome.helix.ui.binding;

/**
 * Runtime values exposed to data-driven UI documents.
 *
 * UI JSON knows binding keys; game code owns how those keys map to state.
 */
public interface UiBindingSource {
    String text(String key);

    float number(String key);

    boolean bool(String key);
}
