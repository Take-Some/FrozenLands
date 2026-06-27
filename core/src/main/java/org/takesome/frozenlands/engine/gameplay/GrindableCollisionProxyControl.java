package org.takesome.frozenlands.engine.gameplay;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

public final class GrindableCollisionProxyControl extends AbstractControl {
    private final Node proxyNode;
    private final RigidBodyControl rigidBodyControl;
    private final PhysicsSpace physicsSpace;
    private boolean detached;

    public GrindableCollisionProxyControl(Node proxyNode, RigidBodyControl rigidBodyControl, PhysicsSpace physicsSpace) {
        this.proxyNode = proxyNode;
        this.rigidBodyControl = rigidBodyControl;
        this.physicsSpace = physicsSpace;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        Spatial previous = this.spatial;
        super.setSpatial(spatial);
        if (previous != null && spatial == null) {
            detachProxy();
        }
    }

    public void detachProxy() {
        if (detached) {
            return;
        }
        detached = true;
        if (physicsSpace != null && rigidBodyControl != null) {
            physicsSpace.remove(rigidBodyControl);
        }
        if (proxyNode != null) {
            proxyNode.removeFromParent();
        }
    }

    public Node proxyNode() {
        return proxyNode;
    }

    public RigidBodyControl rigidBodyControl() {
        return rigidBodyControl;
    }

    @Override
    protected void controlUpdate(float tpf) {
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
