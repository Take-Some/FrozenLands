package dev.takesome.helix.ui.command;

/** Dispatches typed UI commands to engine, Lua or native handlers. */
@FunctionalInterface
public interface UiCommandBus {
    void dispatch(UiCommand command);
}
