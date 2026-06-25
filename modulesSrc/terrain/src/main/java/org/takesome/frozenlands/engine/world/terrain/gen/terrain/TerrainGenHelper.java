package org.takesome.frozenlands.engine.world.terrain.gen.terrain;

import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.*;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.config.Constants;
import org.takesome.frozenlands.engine.world.terrain.chunk.TerrainChunkTracker;
import org.takesome.frozenlands.engine.world.terrain.gen.tree.TreeGen;

import java.util.List;

public class TerrainGenHelper {

    private EngineContext kernelInterface;
    private  TerrainQuad terrain;
    private final TerrainChunkTracker chunkTracker;

    public TerrainGenHelper(EngineContext kernelInterface, TerrainQuad terrain, TerrainChunkTracker chunkTracker){
        this.kernelInterface = kernelInterface;
        this.terrain = terrain;
        this.chunkTracker = chunkTracker;
    }

    void setupScale() {
        terrain.setLocalScale(Constants.TERRAIN_SCALE_X, Constants.TERRAIN_SCALE_Y,
                Constants.TERRAIN_SCALE_Z);
    }

    void setupPosition() {
        terrain.setLocalTranslation(0, 0, 0);
    }

    void setUpLODControl() {
        TerrainLodControl control =
                new TerrainGridLodControl(this.terrain, kernelInterface.getCamera());
        control.setLodCalculator(
                new DistanceLodCalculator(Constants.TERRAIN_LOD_PATCH_SIZE, Constants.TERRAIN_LOD_MULTIPLIER));
        this.terrain.addControl(control);
    }

    void setUpCollision() {
        TreeGen treeGen = new TreeGen(kernelInterface);
        ((TerrainGrid) terrain).addListener(new TerrainGridListener() {
            @Override
            public void gridMoved(Vector3f newCenter) {}

            @Override
            public void tileAttached(Vector3f cell, TerrainQuad quad) {
                chunkTracker.tileAttached(cell, quad);
                installTileRuntime(quad, treeGen, "attached");
            }

            @Override
            public void tileDetached(Vector3f cell, TerrainQuad quad) {
                chunkTracker.tileDetached(cell, quad);
                uninstallTileRuntime(quad);
            }
        });
        installExistingTileRuntime(treeGen);
    }

    private void installExistingTileRuntime(TreeGen treeGen) {
        for (Spatial child : terrain.getChildren()) {
            if (child instanceof TerrainQuad quad) {
                installTileRuntime(quad, treeGen, "existing");
            }
        }
    }

    private void installTileRuntime(TerrainQuad quad, TreeGen treeGen, String reason) {
        replaceTerrainCollision(quad);
        treeGen.positionTrees(quad);
        kernelInterface.getLogger().debug("Terrain tile runtime installed reason={} quad={}", reason, quad.getName());
    }

    private void replaceTerrainCollision(TerrainQuad quad) {
        RigidBodyControl existingControl = quad.getControl(RigidBodyControl.class);
        if (existingControl != null) {
            kernelInterface.getBulletAppState().getPhysicsSpace().remove(existingControl);
            quad.removeControl(RigidBodyControl.class);
        }
        quad.addControl(new RigidBodyControl(
                new HeightfieldCollisionShape(
                        quad.getHeightMap(), terrain.getLocalScale()),
                0));
        kernelInterface.getBulletAppState().getPhysicsSpace().add(quad);
    }

    private void uninstallTileRuntime(TerrainQuad quad) {
        RigidBodyControl terrainControl = quad.getControl(RigidBodyControl.class);
        if (terrainControl != null) {
            kernelInterface.getBulletAppState().getPhysicsSpace().remove(terrainControl);
            quad.removeControl(RigidBodyControl.class);
        }

        List<Spatial> quadForest = quad.getUserData("quadForest");
        if (quadForest == null || quadForest.isEmpty()) {
            return;
        }

        for (Spatial treeNode : quadForest) {
            RigidBodyControl treeControl = treeNode.getControl(RigidBodyControl.class);
            if (treeControl != null) {
                kernelInterface.getBulletAppState().getPhysicsSpace().remove(treeControl);
                treeNode.removeControl(RigidBodyControl.class);
            }
            treeNode.removeFromParent();
            kernelInterface.getLogger().debug("Detached tree " + treeNode.hashCode() + treeNode.getLocalTranslation().toString());
        }
    }
}
