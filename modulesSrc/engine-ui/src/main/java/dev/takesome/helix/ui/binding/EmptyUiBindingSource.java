package dev.takesome.helix.ui.binding;

public final class EmptyUiBindingSource implements UiBindingSource {
    public static final EmptyUiBindingSource INSTANCE = new EmptyUiBindingSource();

    private EmptyUiBindingSource() {
    }

    @Override
    public String text(String key) {
        return "";
    }

    @Override
    public float number(String key) {
        return 0f;
    }

    @Override
    public boolean bool(String key) {
        return false;
    }
}
