package org.takesome.frozenlands.engine.events;

public final class EngineEventTopics {
    public static final String CONSOLE_VISIBILITY_CHANGED = "core.console.visibility.changed";
    public static final String CONSOLE_OPENED = "core.console.opened";
    public static final String CONSOLE_CLOSED = "core.console.closed";

    public static final String CURSOR_VISIBILITY_REQUESTED = "engine.cursor.visibility.requested";
    public static final String CAMERA_FOLLOW_PAUSE_REQUESTED = "engine.camera.follow.pause.requested";
    public static final String CAMERA_LOOK_INPUT_ENABLED_REQUESTED = "engine.camera.look.input.enabled.requested";
    public static final String APPLICATION_FOCUS_CHANGED = "engine.application.focus.changed";
    public static final String ENGINE_TASK_SUBMITTED = "engine.task.submitted";
    public static final String ENGINE_TASK_STARTED = "engine.task.started";
    public static final String ENGINE_TASK_COMPLETED = "engine.task.completed";
    public static final String ENGINE_TASK_FAILED = "engine.task.failed";
    public static final String ENGINE_TASK_CANCELLED = "engine.task.cancelled";
    public static final String ENGINE_TASK_REJECTED = "engine.task.rejected";
    public static final String SHADER_PIPELINE_INITIALIZED = "engine.shaders.pipeline.initialized";
    public static final String SHADER_PIPELINE_ENABLED_CHANGED = "engine.shaders.pipeline.enabled.changed";
    public static final String SHADER_EFFECT_ENABLED_CHANGED = "engine.shaders.effect.enabled.changed";
    public static final String SHADER_SETTINGS_CHANGED = "engine.shaders.settings.changed";
    public static final String APPLICATION_FOCUS_PAUSE_REQUESTED = "engine.application.focus.pause.requested";
    public static final String GAME_MENU_VISIBILITY_CHANGED = "engine.game.menu.visibility.changed";
    public static final String ENGINE_SOUND_PLAY_REQUESTED = "engine.sound.play.requested";
    public static final String ENGINE_SOUND_PLAYED = "engine.sound.played";
    public static final String ENGINE_SOUND_PLAY_FAILED = "engine.sound.play.failed";
    public static final String PARTICLE_EFFECT_REQUESTED = "engine.particles.effect.requested";

    public static final String PLAYER_INPUT_ENABLED_REQUESTED = "player.input.enabled.requested";
    public static final String PLAYER_INPUT_MAPPING_REGISTERED = "player.input.mapping.registered";
    public static final String PLAYER_INPUT_ACTION = "player.input.action";
    public static final String PLAYER_INPUT_ANALOG = "player.input.analog";
    public static final String PLAYER_MOVE_INTENT = "player.move.intent";
    public static final String PLAYER_LOOK_INTENT = "player.look.intent";
    public static final String PLAYER_LOOK_SENSITIVITY_CHANGED = "player.look.sensitivity.changed";
    public static final String PLAYER_HEAD_TURN_REQUESTED = "player.head.turn.requested";
    public static final String PLAYER_HEAD_TURN_CHANGED = "player.head.turn.changed";
    public static final String PLAYER_JUMP_REQUESTED = "player.jump.requested";
    public static final String PLAYER_ATTACK_REQUESTED = "player.attack.requested";
    public static final String PLAYER_RUN_CHANGED = "player.run.changed";
    public static final String PLAYER_STATE_CHANGED = "player.state.changed";
    public static final String PLAYER_CAMERA_VIEW_TOGGLE_REQUESTED = "player.camera.view.toggle.requested";
    public static final String PLAYER_CAMERA_VIEW_REQUESTED = "player.camera.view.requested";
    public static final String PLAYER_CAMERA_VIEW_CHANGED = "player.camera.view.changed";
    public static final String PLAYER_ANIMATION_CHANGED = "player.animation.changed";
    public static final String PLAYER_MANAGER_READY = "player.manager.ready";
    public static final String PLAYER_MANAGER_TELEMETRY = "player.manager.telemetry";
    public static final String PLAYER_ACTIVE_CHANGED = "player.active.changed";
    public static final String PLAYER_SPAWNED = "player.spawned";
    public static final String PLAYER_WARPED = "player.warped";
    public static final String PLAYER_FOOTSTEP = "player.footstep";
    public static final String PLAYER_TAKEOFF = "player.takeoff";
    public static final String PLAYER_LANDED = "player.landed";
    public static final String PLAYER_TOOL_EQUIPPED = "player.tool.equipped";
    public static final String PLAYER_TOOL_SWING = "player.tool.swing";
    public static final String PLAYER_TOOL_MISSED = "player.tool.missed";
    public static final String PLAYER_GRINDABLE_HIT = "player.grindable.hit";
    public static final String PLAYER_GRINDABLE_DESTROYED = "player.grindable.destroyed";
    public static final String PLAYER_TREE_HIT = PLAYER_GRINDABLE_HIT;
    public static final String PLAYER_TREE_CHOPPED = PLAYER_GRINDABLE_DESTROYED;

    private EngineEventTopics() {
    }
}
