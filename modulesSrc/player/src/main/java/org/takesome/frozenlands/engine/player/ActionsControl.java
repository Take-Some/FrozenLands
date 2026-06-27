package org.takesome.frozenlands.engine.player;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import org.takesome.frozenlands.engine.events.EngineEventTopics;

import java.util.Map;

public class ActionsControl extends AbstractControl {
    private final Player playerInterface;
    private final PlayerSoundProvider playerSoundProvider;
    private boolean ready = false;
    private BetterCharacterControl character;
    private boolean wasOnGround = true;

    public ActionsControl(Player playerInterface) {
        this.playerInterface = playerInterface;
        this.character = playerInterface.getPlayerOptions().getCharacterControl();
        this.playerSoundProvider = playerInterface.getPlayerSoundProvider();
    }

    private void initialize() {
        if (ready) {
            return;
        }
        ready = true;
        if (character == null) {
            character = spatial.getControl(BetterCharacterControl.class);
        }
        if (character == null) {
            playerInterface.getLogger().warn("ActionsControl is attached without BetterCharacterControl: " + spatial.getName());
        }
    }

    public Spatial shot(AssetManager assetManager, Vector3f pos, Vector3f direction, Node parent, PhysicsSpace phy) {
        Node bullet = new Node("bullet");
        parent.attachChild(bullet);

        SphereCollisionShape shape = new SphereCollisionShape(0.5f);
        RigidBodyControl rb = new RigidBodyControl(shape, 0.1f);
        bullet.addControl(rb);
        phy.add(rb);
        rb.setGravity(Vector3f.ZERO);
        rb.setPhysicsLocation(pos);
        rb.setLinearVelocity(direction.mult(10f));
        return bullet;
    }

    @Override
    protected void controlUpdate(float tpf) {
        initialize();
        if (!ready || character == null || playerSoundProvider == null) {
            return;
        }

        boolean onGround = character.isOnGround();
        if (!onGround && wasOnGround) {
            playerInterface.publishEvent(EngineEventTopics.PLAYER_TAKEOFF, Map.of(
                    "playerRef", playerInterface.runtimeId(),
                    "velocityY", character.getVelocity().y,
                    "tpf", tpf
            ));
        }
        if (onGround && !wasOnGround) {
            playerInterface.publishEvent(EngineEventTopics.PLAYER_LANDED, Map.of(
                    "playerRef", playerInterface.runtimeId(),
                    "velocityY", character.getVelocity().y,
                    "tpf", tpf
            ));
        }
        wasOnGround = onGround;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}
