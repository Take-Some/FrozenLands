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
import java.util.stream.Stream;

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
        ((TerrainGrid) terrain).addListener(new TerrainGridListener() {
            @Override
            public void gridMoved(Vector3f newCenter) {}

            @Override
            public void tileAttached(Vector3f cell, TerrainQuad quad) {
                chunkTracker.tileAttached(cell, quad);
                TreeGen treeGen = new TreeGen(kernelInterface);
                while (quad.getControl(RigidBodyControl.class) != null) {
                    quad.removeControl(RigidBodyControl.class);
                }
                quad.addControl(new RigidBodyControl(
                        new HeightfieldCollisionShape(
                                quad.getHeightMap(), terrain.getLocalScale()),
                        0));
                kernelInterface.getBulletAppState().getPhysicsSpace().add(quad);
                treeGen.positionTrees(quad);
            }

            @Override
            public void tileDetached(Vector3f cell, TerrainQuad quad) {
                chunkTracker.tileDetached(cell, quad);
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
        });
    }
}
