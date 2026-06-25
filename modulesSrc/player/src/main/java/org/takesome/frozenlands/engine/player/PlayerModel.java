package org.takesome.frozenlands.engine.player;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class PlayerModel extends Node {

    private Spatial playerSpatial;
    private Geometry playerGeometry;

    public PlayerModel(AssetManager assetManager, PlayerOptions playerOptions) {
        playerSpatial = assetManager.loadModel(playerOptions.getModelPath());
        playerSpatial.setLocalScale(playerOptions.getScale());
        attachChild(playerSpatial);
        this.calculateGeometry();
    }

    private void calculateGeometry() {
        if (playerSpatial instanceof Geometry) {
            this.playerGeometry = (Geometry) playerSpatial;
        } else if (playerSpatial instanceof Node) {
            Node playerNode = (Node) playerSpatial;
            for (Spatial child : playerNode.getChildren()) {
                if (child instanceof Geometry) {
                    this.playerGeometry = (Geometry) child;
                }
            }
        }
    }

    public void setCullHint(Spatial.CullHint cullHint) {
        playerSpatial.setCullHint(cullHint);
    }

    public void setShadowMode(RenderQueue.ShadowMode shadowMode) {
        playerSpatial.setShadowMode(shadowMode);
    }

    public Spatial getPlayerSpatial() {
        return playerSpatial;
    }
    public Geometry getPlayerGeometry(){ return  this.playerGeometry;}

}
