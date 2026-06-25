package org.takesome.frozenlands.engine.player.camera;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.player.input.PlayerState;
import org.takesome.frozenlands.engine.player.input.UserInputHandler;

import java.util.Map;

public class CameraFollowSpatial extends AbstractControl {
    private boolean ready = false;
    private Camera cam;
    private UserInputHandler userInputHandler;
    private EngineContext context;
    private Vector3f offset, direction;
    private BetterCharacterControl character;
    private Spatial cameraNode;
    private AutoCloseable cameraPauseSubscription;
    private boolean cameraFollowPaused;

    public CameraFollowSpatial(UserInputHandler userInputHandler, Camera cam) {
        this(userInputHandler, cam, null);
    }

    public CameraFollowSpatial(UserInputHandler userInputHandler, Camera cam, EngineContext context) {
        this.cam = cam;
        this.userInputHandler = userInputHandler;
        this.context = context;
    }

    public void setOffset(Vector3f offset) {
        this.offset = offset;
    }

    public void setDirection(Vector3f direction) {
        this.direction = direction;
    }

    private void initialize() {
        if (ready) return; // initialize only once.
        ready = true;
        subscribeCameraPause();

        if (offset == null) {
            // Offset is null, search if there is a 'camera' node in this spatial
            if (spatial instanceof Node) {
                Node n = (Node) spatial;
                Spatial cameraNode = n.getChild("camera");
                if (cameraNode != null) {
                    this.cameraNode = cameraNode;
                }
            }
        }

        // Find character control in the spatial
        if (character == null) {
            spatial.depthFirstTraversal(sx -> {
                BetterCharacterControl c = sx.getControl(BetterCharacterControl.class);
                if (c != null) character = c;
            });
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        initialize(); // initialize if needed.
        if (cameraFollowPaused) {
            return;
        }
        Vector3f loc = cam.getLocation();
        if (offset != null) {
            // Location is spatial location + offset
            loc.set(spatial.getWorldTranslation());
            loc.addLocal(offset);
        } else {
            // Location is the camera node location
            loc.set(cameraNode.getWorldTranslation());
        }
        cam.setLocation(loc);//.add(new Vector3f(-10,0,0)

        // if character exists: Direction is character direction
        if (character != null) {
            if(!userInputHandler.getPlayerState().equals(PlayerState.STANDING)){
                
            }
            cam.lookAtDirection(character.getViewDirection(), Vector3f.UNIT_Y);
        } else if (cameraNode != null) { // If cameranode exists: Direction is cameranode direction
            cam.setRotation(cameraNode.getWorldRotation());
        } else { // direction is the specified direction
            cam.lookAtDirection(direction, Vector3f.UNIT_Y);
        }
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        if (spatial == null) {
            unsubscribeCameraPause();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    private void subscribeCameraPause() {
        if (context == null || cameraPauseSubscription != null) {
            return;
        }
        cameraPauseSubscription = context.getModuleRegistry().getEventBus().subscribe(
                EngineEventTopics.CAMERA_FOLLOW_PAUSE_REQUESTED,
                this::onCameraPauseRequested,
                true
        );
    }

    private void unsubscribeCameraPause() {
        if (cameraPauseSubscription == null) {
            return;
        }
        try {
            cameraPauseSubscription.close();
        } catch (Exception ignored) {
        }
        cameraPauseSubscription = null;
    }

    private void onCameraPauseRequested(Map<String, Object> event) {
        Object payload = event == null ? null : event.get("payload");
        if (payload instanceof Map<?, ?> map) {
            cameraFollowPaused = Boolean.TRUE.equals(map.get("paused"));
        }
    }
}