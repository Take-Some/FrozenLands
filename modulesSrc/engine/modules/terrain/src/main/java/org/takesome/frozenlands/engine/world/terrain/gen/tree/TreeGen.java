package org.takesome.frozenlands.engine.world.terrain.gen.tree;

import static org.takesome.frozenlands.engine.config.Constants.RAY_DOWN;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.util.ArrayList;
import java.util.List;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.config.Constants;
import org.takesome.frozenlands.engine.utils.LodUtils;
import org.takesome.frozenlands.engine.utils.Utils;

public class TreeGen {
    private static final String QUAD_FOREST_USER_DATA = "quadForest";
    private static final int MIN_TREES_PER_TILE = 80;
    private static final int MAX_TREES_PER_TILE = 160;
    private static final int MAX_PLACEMENT_ATTEMPTS = 8;
    private static final float RAY_START_HEIGHT = 800f;

    private Spatial treeModel;
    private EngineContext kernelInterface;

    public TreeGen(EngineContext kernelInterface) {
        this.kernelInterface = kernelInterface;
        this.initializeTreeModel();
    }

    private void initializeTreeModel() {
        treeModel = kernelInterface.getAssetManager().loadModel("Models/Fir1/fir1_androlo.j3o");
        treeModel.setShadowMode(RenderQueue.ShadowMode.Cast);
        LodUtils.setUpTreeModelLod(treeModel);
    }

    private RigidBodyControl createCollisionControl(Spatial spatial) {
        RigidBodyControl existingControl = spatial.getControl(RigidBodyControl.class);
        if (existingControl != null) {
            kernelInterface.getBulletAppState().getPhysicsSpace().remove(existingControl);
            spatial.removeControl(RigidBodyControl.class);
        }

        CollisionShape shape = CollisionShapeFactory.createMeshShape(spatial);
        RigidBodyControl control = new RigidBodyControl(shape, 0);
        spatial.addControl(control);
        kernelInterface.getBulletAppState().getPhysicsSpace().add(control);
        return control;
    }

    public List<Spatial> setupTrees() {
        int forestSize = (int) Utils.getRandomNumberInRange(MIN_TREES_PER_TILE, MAX_TREES_PER_TILE);
        List<Spatial> quadForest = new ArrayList<>(forestSize);
        for (int i = 0; i < forestSize; i++) {
            Spatial treeModelCustom = treeModel.clone();
            float scaleFactor = Utils.getRandomNumberInRange(0.1f, 1.5f);
            treeModelCustom.scale(scaleFactor, scaleFactor, scaleFactor);
            quadForest.add(treeModelCustom);
        }

        return quadForest;
    }

    public void positionTrees(TerrainQuad quad) {
        List<Spatial> quadForest = quad.getUserData(QUAD_FOREST_USER_DATA);
        if (quadForest == null) {
            quadForest = new ArrayList<>();

            for (Spatial treeNode : setupTrees()) {
                if (placeTreeOnQuad(quad, treeNode)) {
                    kernelInterface.getRootNode().attachChild(treeNode);
                    createCollisionControl(treeNode);
                    quadForest.add(treeNode);
                    kernelInterface.getLogger().debug("Attached tree "
                            + treeNode.hashCode()
                            + treeNode.getLocalTranslation().toString());
                }
            }

            quad.setUserData(QUAD_FOREST_USER_DATA, quadForest);
            return;
        }

        for (Spatial treeNode : quadForest) {
            if (treeNode.getParent() == null) {
                kernelInterface.getRootNode().attachChild(treeNode);
            }
            if (treeNode.getControl(RigidBodyControl.class) == null) {
                createCollisionControl(treeNode);
            }
            kernelInterface.getLogger().debug("Attached tree again "
                    + treeNode.hashCode()
                    + treeNode.getLocalTranslation().toString());
        }
    }

    private boolean placeTreeOnQuad(TerrainQuad quad, Spatial treeNode) {
        BoundingBox bounds = getQuadBounds(quad);
        if (bounds == null) {
            kernelInterface.getLogger().warn("Cannot place trees: TerrainQuad has no world bounds: " + quad.getName());
            return false;
        }

        Vector3f center = bounds.getCenter();
        float minX = center.x - bounds.getXExtent();
        float maxX = center.x + bounds.getXExtent();
        float minZ = center.z - bounds.getZExtent();
        float maxZ = center.z + bounds.getZExtent();
        float rayStartY = center.y + Math.max(bounds.getYExtent(), 1f) + RAY_START_HEIGHT;

        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            CollisionResults results = new CollisionResults();
            Vector3f start = new Vector3f(
                    Utils.getRandomNumberInRange(minX, maxX),
                    rayStartY,
                    Utils.getRandomNumberInRange(minZ, maxZ));

            quad.collideWith(new Ray(start, RAY_DOWN), results);
            CollisionResult hit = results.getClosestCollision();
            if (hit != null && hit.getContactPoint().y > Constants.WATER_LEVEL_HEIGHT) {
                Vector3f plantLocation = new Vector3f(hit.getContactPoint().x,
                        hit.getContactPoint().y, hit.getContactPoint().z);
                Quaternion rotation = new Quaternion().fromAngleAxis(
                        Utils.getRandomNumberInRange(-15f, 15f) * FastMath.DEG_TO_RAD,
                        new Vector3f(0, 1, 0));
                treeNode.setLocalTranslation(plantLocation);
                treeNode.setLocalRotation(rotation);
                return true;
            }
        }

        return false;
    }

    private BoundingBox getQuadBounds(TerrainQuad quad) {
        if (quad.getWorldBound() instanceof BoundingBox) {
            return (BoundingBox) quad.getWorldBound();
        }

        return null;
    }
}
