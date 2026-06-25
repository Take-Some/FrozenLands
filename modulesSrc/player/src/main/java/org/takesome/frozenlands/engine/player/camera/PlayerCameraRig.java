package org.takesome.frozenlands.engine.player.camera;

import com.jme3.renderer.Camera;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.player.Player;
import org.takesome.frozenlands.engine.player.input.UserInputHandler;

public final class PlayerCameraRig {
    private final Camera camera;
    private final CameraFollowSpatial followControl;

    public PlayerCameraRig(Player owner, UserInputHandler inputHandler, Camera camera, EngineContext context) {
        this.camera = camera;
        this.followControl = new CameraFollowSpatial(inputHandler, camera, context);
        owner.addControl(followControl);
    }

    public Camera camera() {
        return camera;
    }

    public CameraFollowSpatial followControl() {
        return followControl;
    }
}
