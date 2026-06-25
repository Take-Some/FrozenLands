package org.takesome.frozenlands.engine.events;

public final class EngineEventTopics {
    public static final String CONSOLE_VISIBILITY_CHANGED = "core.console.visibility.changed";
    public static final String CONSOLE_OPENED = "core.console.opened";
    public static final String CONSOLE_CLOSED = "core.console.closed";

    public static final String CURSOR_VISIBILITY_REQUESTED = "engine.cursor.visibility.requested";
    public static final String CAMERA_FOLLOW_PAUSE_REQUESTED = "engine.camera.follow.pause.requested";
    public static final String CAMERA_LOOK_INPUT_ENABLED_REQUESTED = "engine.camera.look.input.enabled.requested";

    public static final String PLAYER_INPUT_ENABLED_REQUESTED = "player.input.enabled.requested";
    public static final String PLAYER_INPUT_MAPPING_REGISTERED = "player.input.mapping.registered";
    public static final String PLAYER_INPUT_ACTION = "player.input.action";
    public static final String PLAYER_INPUT_ANALOG = "player.input.analog";
    public static final String PLAYER_MOVE_INTENT = "player.move.intent";
    public static final String PLAYER_LOOK_INTENT = "player.look.intent";
    public static final String PLAYER_JUMP_REQUESTED = "player.jump.requested";
    public static final String PLAYER_ATTACK_REQUESTED = "player.attack.requested";
    public static final String PLAYER_RUN_CHANGED = "player.run.changed";
    public static final String PLAYER_STATE_CHANGED = "player.state.changed";

    private EngineEventTopics() {
    }
}
