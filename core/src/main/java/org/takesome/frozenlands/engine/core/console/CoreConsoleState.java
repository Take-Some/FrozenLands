package org.takesome.frozenlands.engine.core.console;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventTopics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CoreConsoleState extends BaseAppState implements ActionListener, RawInputListener {
    private static final String TOGGLE_MAPPING = "core.console.toggle";
    private static final int MAX_OUTPUT_LINES = 13;
    private static final float CURSOR_BLINK_INTERVAL_SECONDS = 0.45f;

    private final EngineContext context;
    private final ConsoleInputBuffer input = new ConsoleInputBuffer();
    private final ConsoleHistory history = new ConsoleHistory();
    private final List<String> output = new ArrayList<>();

    private ConsoleOverlayView view;
    private boolean open;
    private float cursorBlinkTimer;
    private boolean cursorVisible = true;

    public CoreConsoleState(EngineContext context) {
        this.context = context;
    }

    @Override
    protected void initialize(Application application) {
        view = new ConsoleOverlayView(context.getAssetManager(), context.getCamera(), context.getGuiNode());
        view.initialize();
        installInput();
        addOutput("FrozenLands core console: F1 toggle, Tab autocomplete, Enter execute.");
        refreshView();
    }

    @Override
    protected void cleanup(Application application) {
        InputManager inputManager = context.getInputManager();
        inputManager.removeListener(this);
        inputManager.removeRawInputListener(this);
        if (inputManager.hasMapping(TOGGLE_MAPPING)) {
            inputManager.deleteMapping(TOGGLE_MAPPING);
        }
        if (view != null) {
            view.dispose();
        }
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update(float tpf) {
        if (!open) {
            return;
        }
        cursorBlinkTimer += tpf;
        if (cursorBlinkTimer >= CURSOR_BLINK_INTERVAL_SECONDS) {
            cursorBlinkTimer = 0f;
            cursorVisible = !cursorVisible;
            refreshView();
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (TOGGLE_MAPPING.equals(name) && isPressed) {
            toggle();
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent event) {
        if (!open || !event.isPressed()) {
            return;
        }
        if (event.getKeyCode() == KeyInput.KEY_F1) {
            return;
        }
        if (handleSpecialKey(event.getKeyCode())) {
            event.setConsumed();
            resetCursorBlink();
            refreshView();
            return;
        }
        input.append(event.getKeyChar());
        event.setConsumed();
        resetCursorBlink();
        refreshView();
    }

    private boolean handleSpecialKey(int keyCode) {
        return switch (keyCode) {
            case KeyInput.KEY_ESCAPE -> {
                setOpen(false);
                yield true;
            }
            case KeyInput.KEY_RETURN -> {
                executeInput();
                yield true;
            }
            case KeyInput.KEY_BACK -> {
                input.backspace();
                yield true;
            }
            case KeyInput.KEY_TAB -> {
                autocomplete();
                yield true;
            }
            case KeyInput.KEY_UP -> {
                input.setLine(history.previous(input.line()));
                yield true;
            }
            case KeyInput.KEY_DOWN -> {
                input.setLine(history.next(input.line()));
                yield true;
            }
            default -> false;
        };
    }

    private void installInput() {
        InputManager inputManager = context.getInputManager();
        if (!inputManager.hasMapping(TOGGLE_MAPPING)) {
            inputManager.addMapping(TOGGLE_MAPPING, new KeyTrigger(KeyInput.KEY_F1));
        }
        inputManager.addListener(this, TOGGLE_MAPPING);
        inputManager.addRawInputListener(this);
    }

    private void toggle() {
        setOpen(!open);
    }

    private void setOpen(boolean open) {
        if (this.open == open) {
            return;
        }
        this.open = open;
        resetCursorBlink();
        publishVisibilityChanged(open);
        view.setVisible(open);
        refreshView();
    }

    private void resetCursorBlink() {
        cursorBlinkTimer = 0f;
        cursorVisible = true;
    }

    private void publishVisibilityChanged(boolean open) {
        context.getModuleRegistry().publishEvent(EngineEventTopics.CONSOLE_VISIBILITY_CHANGED, Map.of("open", open));
        context.getModuleRegistry().publishEvent(open ? EngineEventTopics.CONSOLE_OPENED : EngineEventTopics.CONSOLE_CLOSED, Map.of("open", open));
    }

    private void executeInput() {
        String line = input.line().trim();
        if (line.isBlank()) {
            return;
        }
        history.push(line);
        addOutput("> " + line);
        Map<String, Object> response = context.getModuleRegistry().call("engine.core", "console.execute", Map.of("line", line));
        addOutput(String.valueOf(response));
        input.clear();
    }

    private void autocomplete() {
        String prefix = input.commandToken();
        if (prefix.isBlank()) {
            prefix = "/";
        }
        Map<String, Object> response = context.getModuleRegistry().call("engine.core", "console.complete", Map.of("prefix", prefix));
        Object completion = response.get("completion");
        Object matches = response.get("matches");
        if (completion instanceof String value && !value.isBlank() && !value.equals(prefix)) {
            input.replaceCommandToken(value, true);
        } else if (matches instanceof List<?> list && list.isEmpty()) {
            addOutput("no completion for: " + prefix);
        }
    }

    private String completionHint() {
        if (!open) {
            return "";
        }
        String prefix = input.commandToken();
        if (prefix.isBlank()) {
            prefix = "/";
        }
        Map<String, Object> response = context.getModuleRegistry().call("engine.core", "console.complete", Map.of("prefix", prefix));
        Object matches = response.get("matches");
        if (!(matches instanceof List<?> list) || list.isEmpty()) {
            return "Tab: no matches";
        }
        List<String> names = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> descriptor && descriptor.get("name") != null) {
                names.add(String.valueOf(descriptor.get("name")));
            }
            if (names.size() >= 8) {
                break;
            }
        }
        return "Tab: " + String.join("  ", names);
    }

    private void addOutput(String line) {
        output.add(line);
        while (output.size() > MAX_OUTPUT_LINES) {
            output.remove(0);
        }
    }

    private void refreshView() {
        if (view != null) {
            view.update(input.line(), output, completionHint(), cursorVisible);
        }
    }

    @Override public void beginInput() { }
    @Override public void endInput() { }
    @Override public void onJoyAxisEvent(JoyAxisEvent event) { }
    @Override public void onJoyButtonEvent(JoyButtonEvent event) { }
    @Override public void onMouseMotionEvent(MouseMotionEvent event) { }
    @Override public void onMouseButtonEvent(MouseButtonEvent event) { }
    @Override public void onTouchEvent(TouchEvent event) { }
}
