package org.takesome.frozenlands.engine.core.console;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
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

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CoreConsoleState extends BaseAppState implements ActionListener, RawInputListener {
    private static final String TOGGLE_MAPPING = "core.console.toggle";
    private static final int MAX_OUTPUT_LINES = 80;
    private static final int MAX_COMPLETION_ROWS = 8;
    private static final float CURSOR_BLINK_INTERVAL_SECONDS = 0.45f;

    private final EngineContext context;
    private final ConsoleInputBuffer input = new ConsoleInputBuffer();
    private final ConsoleHistory history = new ConsoleHistory();
    private final List<String> output = new ArrayList<>();
    private final List<CompletionEntry> completions = new ArrayList<>();

    private ConsoleOverlayView view;
    private boolean open;
    private float cursorBlinkTimer;
    private boolean cursorVisible = true;
    private int selectedCompletionIndex = -1;
    private int outputScrollOffset;

    public CoreConsoleState(EngineContext context) {
        this.context = context;
    }

    @Override
    protected void initialize(Application application) {
        view = new ConsoleOverlayView(context.getAssetManager(), context.getCamera(), context.getGuiNode());
        view.initialize();
        installInput();
        addOutput("FrozenLands core console: F1 toggle, Tab completions, arrows select, Enter fill/execute.");
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

    @Override protected void onEnable() { }
    @Override protected void onDisable() { }

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
        Character character = printableCharacter(event);
        if (character != null) {
            input.append(character);
            refreshCompletions();
            event.setConsumed();
            resetCursorBlink();
            refreshView();
        }
    }

    private boolean handleSpecialKey(int keyCode) {
        return switch (keyCode) {
            case KeyInput.KEY_ESCAPE -> {
                if (completionOpen()) {
                    clearCompletions();
                } else {
                    setOpen(false);
                }
                yield true;
            }
            case KeyInput.KEY_RETURN -> {
                if (completionOpen()) {
                    fillSelectedCompletion();
                } else {
                    executeInput();
                }
                yield true;
            }
            case KeyInput.KEY_BACK -> {
                input.backspace();
                refreshCompletions();
                yield true;
            }
            case KeyInput.KEY_TAB -> {
                autocomplete();
                yield true;
            }
            case KeyInput.KEY_UP -> {
                if (completionOpen()) {
                    moveCompletionSelection(-1);
                } else {
                    input.setLine(history.previous(input.line()));
                    refreshCompletions();
                }
                yield true;
            }
            case KeyInput.KEY_DOWN -> {
                if (completionOpen()) {
                    moveCompletionSelection(1);
                } else {
                    input.setLine(history.next(input.line()));
                    refreshCompletions();
                }
                yield true;
            }
            case KeyInput.KEY_PGUP -> {
                scrollOutput(visibleOutputRows());
                yield true;
            }
            case KeyInput.KEY_PGDN -> {
                scrollOutput(-visibleOutputRows());
                yield true;
            }
            case KeyInput.KEY_HOME -> {
                outputScrollOffset = maxOutputScrollOffset();
                yield true;
            }
            case KeyInput.KEY_END -> {
                outputScrollOffset = 0;
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
        if (open) {
            refreshCompletions();
        } else {
            clearCompletions();
        }
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
        try {
            Map<String, Object> response = context.getModuleRegistry().call("engine.core", "console.execute", Map.of("line", line));
            addOutput(String.valueOf(response));
        } catch (RuntimeException exception) {
            addOutput("error: " + exception.getMessage());
        }
        input.clear();
        outputScrollOffset = 0;
        clearCompletions();
    }

    private void autocomplete() {
        refreshCompletions();
        if (!completionOpen()) {
            addOutput("no completion for: " + completionPrefix());
        }
    }

    private void refreshCompletions() {
        completions.clear();
        selectedCompletionIndex = -1;
        if (!open) {
            return;
        }
        String prefix = completionPrefix();
        try {
            Map<String, Object> response = context.getModuleRegistry().call("engine.core", "console.complete", Map.of("prefix", prefix));
            Object matches = response.get("matches");
            if (matches instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> descriptor && descriptor.get("name") != null) {
                        String name = String.valueOf(descriptor.get("name"));
                        String description = descriptor.get("description") == null ? "" : String.valueOf(descriptor.get("description"));
                        completions.add(new CompletionEntry(name, description));
                        if (completions.size() >= MAX_COMPLETION_ROWS) {
                            break;
                        }
                    }
                }
            }
        } catch (RuntimeException exception) {
            completions.clear();
        }
        if (!completions.isEmpty()) {
            selectedCompletionIndex = 0;
        }
    }

    private String completionPrefix() {
        String prefix = input.commandToken();
        return prefix.isBlank() ? "/" : prefix;
    }

    private boolean completionOpen() {
        return !completions.isEmpty() && selectedCompletionIndex >= 0;
    }

    private void clearCompletions() {
        completions.clear();
        selectedCompletionIndex = -1;
    }

    private void moveCompletionSelection(int delta) {
        if (completions.isEmpty()) {
            selectedCompletionIndex = -1;
            return;
        }
        int size = completions.size();
        selectedCompletionIndex = Math.floorMod(selectedCompletionIndex + delta, size);
    }

    private void fillSelectedCompletion() {
        if (!completionOpen()) {
            return;
        }
        CompletionEntry selected = completions.get(selectedCompletionIndex);
        input.replaceCommandToken(selected.name(), true);
        clearCompletions();
    }

    private String completionHint() {
        if (!open) {
            return "";
        }
        if (completionOpen()) {
            return "Autocomplete: Up/Down select, Enter fill, Esc close list";
        }
        return "Tab: show commands | Up/Down: history";
    }

    private List<String> completionRows() {
        List<String> rows = new ArrayList<>();
        for (CompletionEntry entry : completions) {
            String description = entry.description().isBlank() ? "" : "  -  " + entry.description();
            rows.add(entry.name() + description);
        }
        return rows;
    }

    private void clearOutput() {
        output.clear();
        outputScrollOffset = 0;
        addOutput("console cleared");
    }

    private void pasteClipboard() {
        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data instanceof String text && !text.isBlank()) {
                input.append(text.replace("\r", "").replace("\n", " "));
                refreshCompletions();
                addOutput("pasted clipboard text");
            } else {
                addOutput("clipboard is empty");
            }
        } catch (Exception exception) {
            addOutput("clipboard paste failed: " + exception.getClass().getSimpleName());
        }
    }

    private Character printableCharacter(KeyInputEvent event) {
        char typed = event.getKeyChar();
        if (typed >= 32 && typed != 127 && typed != Character.MAX_VALUE) {
            return typed;
        }
        return null;
    }

    private void addOutput(String line) {
        boolean lockedToBottom = outputScrollOffset == 0;
        output.add(line);
        if (!lockedToBottom) {
            outputScrollOffset++;
        }
        while (output.size() > MAX_OUTPUT_LINES) {
            output.remove(0);
            if (outputScrollOffset > 0) {
                outputScrollOffset--;
            }
        }
        clampOutputScrollOffset();
    }

    private void scrollOutput(int delta) {
        outputScrollOffset += delta;
        clampOutputScrollOffset();
    }

    private int maxOutputScrollOffset() {
        return Math.max(0, output.size() - visibleOutputRows());
    }

    private int visibleOutputRows() {
        return view == null ? 8 : Math.max(1, view.visibleOutputLineCount());
    }

    private void clampOutputScrollOffset() {
        outputScrollOffset = Math.max(0, Math.min(outputScrollOffset, maxOutputScrollOffset()));
    }

    private void refreshView() {
        if (view != null) {
            clampOutputScrollOffset();
            view.update(input.line(), output, completionHint(), cursorVisible, completionRows(), selectedCompletionIndex, outputScrollOffset);
        }
    }

    @Override public void beginInput() { }
    @Override public void endInput() { }
    @Override public void onJoyAxisEvent(JoyAxisEvent event) { }
    @Override public void onJoyButtonEvent(JoyButtonEvent event) { }
    @Override
    public void onMouseMotionEvent(MouseMotionEvent event) {
        if (!open || event.getDeltaWheel() == 0) {
            return;
        }
        scrollOutput(event.getDeltaWheel() > 0 ? visibleOutputRows() : -visibleOutputRows());
        event.setConsumed();
        refreshView();
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent event) {
        if (!open || !event.isPressed() || event.getButtonIndex() != MouseInput.BUTTON_LEFT || view == null) {
            return;
        }
        if (view.hitClearButton(event.getX(), event.getY())) {
            clearOutput();
            event.setConsumed();
            resetCursorBlink();
            refreshView();
            return;
        }
        if (view.hitPasteButton(event.getX(), event.getY())) {
            pasteClipboard();
            event.setConsumed();
            resetCursorBlink();
            refreshView();
        }
    }

    @Override public void onTouchEvent(TouchEvent event) { }

    private record CompletionEntry(String name, String description) {
    }
}
