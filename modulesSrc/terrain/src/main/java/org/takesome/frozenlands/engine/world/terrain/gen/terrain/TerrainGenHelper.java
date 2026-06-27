package org.takesome.frozenlands.engine.world.terrain.gen.terrain;

import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainGrid;
import com.jme3.terrain.geomipmap.TerrainGridListener;
import com.jme3.terrain.geomipmap.TerrainGridLodControl;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.gameplay.GrindableCollisionProxyControl;
import org.takesome.frozenlands.engine.world.terrain.TerrainRuntimeSettings;
import org.takesome.frozenlands.engine.world.terrain.chunk.TerrainChunkTracker;
import org.takesome.frozenlands.engine.world.terrain.gen.tree.TreeGen;

import java.util.List;

public class TerrainGenHelper {
    private final EngineContext kernelInterface;
    private final TerrainQuad terrain;
    private final TerrainChunkTracker chunkTracker;
    private final TerrainRuntimeSettings settings;

    public TerrainGenHelper(EngineContext kernelInterface, TerrainQuad terrain, TerrainChunkTracker chunkTracker) {
        this.kernelInterface = kernelInterface;
        this.terrain = terrain;
        this.chunkTracker = chunkTracker;
        this.settings = new TerrainRuntimeSettings();
    }

    void setupScale() {
        terrain.setLocalScale(settings.scaleX(), settings.scaleY(), settings.scaleZ());
    }

    void setupPosition() {
        terrain.setLocalTranslation(0, 0, 0);
    }

    void setUpLODControl() {
        TerrainLodControl control = new TerrainGridLodControl(this.terrain, kernelInterface.getCamera());
        control.setLodCalculator(new DistanceLodCalculator(settings.lodPatchSize(), settings.lodMultiplier()));
        this.terrain.addControl(control);
    }

    void setUpCollision() {
        TreeGen treeGen = new TreeGen(kernelInterface);
        ((TerrainGrid) terrain).addListener(new TerrainGridListener() {
            @Override
            public void gridMoved(Vector3f newCenter) {
            }

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

        List<Spatial> quadAssets = quad.getUserData(TreeGen.QUAD_ASSET_USER_DATA);
        if (quadAssets == null || quadAssets.isEmpty()) {
            quadAssets = quad.getUserData(TreeGen.LEGACY_QUAD_FOREST_USER_DATA);
        }
        if (quadAssets == null || quadAssets.isEmpty()) {
            return;
        }

        for (Spatial assetNode : quadAssets) {
            RigidBodyControl treeControl = assetNode.getControl(RigidBodyControl.class);
            if (treeControl != null) {
                kernelInterface.getBulletAppState().getPhysicsSpace().remove(treeControl);
                assetNode.removeControl(RigidBodyControl.class);
            }
            GrindableCollisionProxyControl proxyControl = assetNode.getControl(GrindableCollisionProxyControl.class);
            if (proxyControl != null) {
                proxyControl.detachProxy();
                assetNode.removeControl(proxyControl);
            }
            assetNode.removeFromParent();
            kernelInterface.getLogger().debug("Detached terrain asset {} at {}", assetNode.hashCode(), assetNode.getLocalTranslation());
        }
    }
}
