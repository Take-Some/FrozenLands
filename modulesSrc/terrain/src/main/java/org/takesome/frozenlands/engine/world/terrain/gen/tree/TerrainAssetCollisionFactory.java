package org.takesome.frozenlands.engine.world.terrain.gen.tree;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.gameplay.GameplayUserData;
import org.takesome.frozenlands.engine.gameplay.GrindableCollisionProxyControl;
import org.takesome.frozenlands.engine.world.terrain.TerrainPlacementGroup;

final class TerrainAssetCollisionFactory {
    private final EngineContext context;

    TerrainAssetCollisionFactory(EngineContext context) {
        this.context = context;
    }

    void ensureCollision(Spatial spatial, TerrainPlacementGroup group) {
        if (spatial == null || group == null || !group.collisionEnabled()) {
            return;
        }
        if (Boolean.TRUE.equals(spatial.getUserData(GameplayUserData.GRINDABLE_DESTROYED))) {
            return;
        }
        if (spatial.getControl(RigidBodyControl.class) == null
                && spatial.getControl(GrindableCollisionProxyControl.class) == null) {
            createCollisionControl(spatial, group);
        }
    }

    private RigidBodyControl createCollisionControl(Spatial spatial, TerrainPlacementGroup group) {
        removeExistingCollision(spatial);
        if (group.collisionMesh()) {
            return createMeshCollisionControl(spatial);
        }
        return createTrunkProxyCollisionControl(spatial, group);
    }

    private void removeExistingCollision(Spatial spatial) {
        RigidBodyControl existingControl = spatial.getControl(RigidBodyControl.class);
        if (existingControl != null) {
            context.getBulletAppState().getPhysicsSpace().remove(existingControl);
            spatial.removeControl(RigidBodyControl.class);
        }
        GrindableCollisionProxyControl proxyControl
                = spatial.getControl(GrindableCollisionProxyControl.class);
        if (proxyControl != null) {
            proxyControl.detachProxy();
            spatial.removeControl(proxyControl);
        }
    }

    private RigidBodyControl createMeshCollisionControl(Spatial spatial) {
        CollisionShape shape = CollisionShapeFactory.createMeshShape(spatial);
        RigidBodyControl control = new RigidBodyControl(shape, 0);
        spatial.addControl(control);
        context.getBulletAppState().getPhysicsSpace().add(control);
        return control;
    }

    private RigidBodyControl createTrunkProxyCollisionControl(
            Spatial spatial,
            TerrainPlacementGroup group
    ) {
        BoundingBox box = boundsOf(spatial);
        Vector3f worldLocation = spatial.getWorldTranslation().clone();
        float radius = group.proxyRadiusMin();
        float halfHeight = group.proxyHalfHeightMin();
        if (box != null) {
            Vector3f center = box.getCenter();
            float bottom = center.y - box.getYExtent();
            radius = clamp(
                    Math.min(box.getXExtent(), box.getZExtent()) * group.proxyRadiusFactor(),
                    group.proxyRadiusMin(),
                    group.proxyRadiusMax()
            );
            halfHeight = clamp(
                    box.getYExtent() * group.proxyHalfHeightFactor(),
                    group.proxyHalfHeightMin(),
                    group.proxyHalfHeightMax()
            );
            worldLocation.y = bottom + halfHeight + group.proxyYOffset();
        } else {
            worldLocation.y += halfHeight;
        }

        BoxCollisionShape shape = new BoxCollisionShape(new Vector3f(radius, halfHeight, radius));
        RigidBodyControl control = new RigidBodyControl(shape, 0);
        Node proxyNode = new Node("terrain_asset_collision_" + System.identityHashCode(spatial));
        proxyNode.setLocalTranslation(worldLocation);
        proxyNode.setLocalRotation(spatial.getWorldRotation());
        proxyNode.addControl(control);
        context.getRootNode().attachChild(proxyNode);
        context.getBulletAppState().getPhysicsSpace().add(control);
        spatial.addControl(new GrindableCollisionProxyControl(
                proxyNode,
                control,
                context.getBulletAppState().getPhysicsSpace()
        ));
        return control;
    }

    private BoundingBox boundsOf(Spatial spatial) {
        spatial.updateModelBound();
        spatial.updateGeometricState();
        BoundingVolume bound = spatial.getWorldBound();
        return bound instanceof BoundingBox box ? box : null;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
