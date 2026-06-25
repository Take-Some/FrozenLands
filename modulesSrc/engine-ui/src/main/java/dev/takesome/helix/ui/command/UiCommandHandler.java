package dev.takesome.helix.ui.command;

@FunctionalInterface
public interface UiCommandHandler {
    void handle(UiCommand command);
}
