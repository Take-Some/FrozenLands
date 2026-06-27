package org.takesome.frozenlands.engine.player;

import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.player.input.PlayerState;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerLocomotionState {
    private PlayerState state = PlayerState.STANDING;
    private final Vector3f walkDirection = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    private boolean onGround = true;
    private boolean running;
    private float currentSpeed;
    private float horizontalSpeed;

    public PlayerState state() { return state; }
    public Vector3f walkDirection() { return walkDirection.clone(); }
    public Vector3f velocity() { return velocity.clone(); }
    public boolean onGround() { return onGround; }
    public boolean running() { return running; }
    public float currentSpeed() { return currentSpeed; }
    public float horizontalSpeed() { return horizontalSpeed; }

    public void update(PlayerState state, Vector3f walkDirection, Vector3f velocity, boolean onGround, boolean running, float currentSpeed) {
        this.state = state == null ? PlayerState.STANDING : state;
        this.walkDirection.set(walkDirection == null ? Vector3f.ZERO : walkDirection);
        this.velocity.set(velocity == null ? Vector3f.ZERO : velocity);
        this.onGround = onGround;
        this.running = running;
        this.currentSpeed = currentSpeed;
        this.horizontalSpeed = (float) Math.sqrt(this.velocity.x * this.velocity.x + this.velocity.z * this.velocity.z);
    }

    public Map<String, Object> toMap(int playerRef) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("playerRef", playerRef);
        map.put("state", state.name());
        map.put("onGround", onGround);
        map.put("running", running);
        map.put("currentSpeed", currentSpeed);
        map.put("horizontalSpeed", horizontalSpeed);
        map.put("walkX", walkDirection.x);
        map.put("walkY", walkDirection.y);
        map.put("walkZ", walkDirection.z);
        map.put("velocityX", velocity.x);
        map.put("velocityY", velocity.y);
        map.put("velocityZ", velocity.z);
        return map;
    }
}
