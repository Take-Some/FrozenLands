package dev.takesome.helix.ui.input;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
/**
 * Engine-level UI input event used by retained-mode scene nodes.
 *
 * <p>The event is intentionally backend-neutral: LibGDX, desktop, editor and
 * future Aurelia/Vulkan providers can map their native input into this shape
 * before dispatching it through a Scene -> Node tree.</p>
 */
public final class UiInputEvent {
    public enum Type {
        MOUSE_MOVE,
        MOUSE_DOWN,
        MOUSE_UP,
        MOUSE_CLICK,
        MOUSE_SCROLL,
        KEY_DOWN,
        KEY_UP,
        TEXT_INPUT
    }

    public enum MouseButton {
        NONE,
        LEFT,
        MIDDLE,
        RIGHT
    }

    private final Type type;
    private final float mouseX;
    private final float mouseY;
    private final MouseButton mouseButton;
    private final int keyCode;
    private final String text;
    private final float scrollX;
    private final float scrollY;
    private boolean consumed;

    private UiInputEvent(Type type, float mouseX, float mouseY, MouseButton mouseButton, int keyCode, String text) {
        this(type, mouseX, mouseY, mouseButton, keyCode, text, 0f, 0f);
    }

    private UiInputEvent(Type type, float mouseX, float mouseY, MouseButton mouseButton, int keyCode, String text, float scrollX, float scrollY) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        this.type = type;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mouseButton = mouseButton == null ? MouseButton.NONE : mouseButton;
        this.keyCode = keyCode;
        this.text = emptyIfNull(text);
        this.scrollX = scrollX;
        this.scrollY = scrollY;
    }

    public static UiInputEvent mouseMove(float x, float y) {
        return new UiInputEvent(Type.MOUSE_MOVE, x, y, MouseButton.NONE, 0, "");
    }

    public static UiInputEvent mouseDown(float x, float y) {
        return mouseDown(x, y, MouseButton.LEFT);
    }

    public static UiInputEvent mouseDown(float x, float y, MouseButton button) {
        return new UiInputEvent(Type.MOUSE_DOWN, x, y, button, 0, "");
    }

    public static UiInputEvent mouseUp(float x, float y) {
        return mouseUp(x, y, MouseButton.LEFT);
    }

    public static UiInputEvent mouseUp(float x, float y, MouseButton button) {
        return new UiInputEvent(Type.MOUSE_UP, x, y, button, 0, "");
    }

    public static UiInputEvent mouseClick(float x, float y) {
        return mouseClick(x, y, MouseButton.LEFT);
    }

    public static UiInputEvent mouseClick(float x, float y, MouseButton button) {
        return new UiInputEvent(Type.MOUSE_CLICK, x, y, button, 0, "");
    }


    public static UiInputEvent mouseScroll(float x, float y, float amountX, float amountY) {
        return new UiInputEvent(Type.MOUSE_SCROLL, x, y, MouseButton.NONE, 0, "", amountX, amountY);
    }

    public static UiInputEvent keyDown(int keyCode) {
        return new UiInputEvent(Type.KEY_DOWN, 0f, 0f, MouseButton.NONE, keyCode, "");
    }

    public static UiInputEvent keyUp(int keyCode) {
        return new UiInputEvent(Type.KEY_UP, 0f, 0f, MouseButton.NONE, keyCode, "");
    }

    public static UiInputEvent textInput(String text) {
        return new UiInputEvent(Type.TEXT_INPUT, 0f, 0f, MouseButton.NONE, 0, text);
    }

    public Type type() {
        return type;
    }

    public float mouseX() {
        return mouseX;
    }

    public float mouseY() {
        return mouseY;
    }

    public MouseButton mouseButton() {
        return mouseButton;
    }

    public int keyCode() {
        return keyCode;
    }

    public String text() {
        return text;
    }


    public float scrollX() {
        return scrollX;
    }

    public float scrollY() {
        return scrollY;
    }

    public boolean isMouseMove() {
        return type == Type.MOUSE_MOVE;
    }

    public boolean isMouseDown() {
        return type == Type.MOUSE_DOWN;
    }

    public boolean isMouseUp() {
        return type == Type.MOUSE_UP;
    }

    public boolean isMouseClick() {
        return type == Type.MOUSE_CLICK;
    }

    public boolean isMouseScroll() {
        return type == Type.MOUSE_SCROLL;
    }

    public boolean isPointerEvent() {
        return type == Type.MOUSE_MOVE || type == Type.MOUSE_DOWN || type == Type.MOUSE_UP || type == Type.MOUSE_CLICK || type == Type.MOUSE_SCROLL;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void consume() {
        consumed = true;
    }
}
