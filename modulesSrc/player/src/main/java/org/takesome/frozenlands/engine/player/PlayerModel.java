package org.takesome.frozenlands.engine.player;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class PlayerModel extends Node {
    private final PlayerOptions playerOptions;
    private Spatial playerSpatial;
    private Geometry playerGeometry;
    private Spatial collisionSpatial;
    private PlayerCollisionProfile collisionProfile;

    public PlayerModel(AssetManager assetManager, PlayerOptions playerOptions) {
        this.playerOptions = playerOptions;
        this.playerSpatial = assetManager.loadModel(playerOptions.getModelPath());
        this.playerSpatial.setLocalScale(playerOptions.getScale());
        attachChild(playerSpatial);
        refreshModelBounds();
        this.playerGeometry = findFirstGeometry(playerSpatial);
        this.collisionSpatial = findNamedSpatial(playerSpatial, playerOptions.getCollisionSourceNode());
        this.collisionProfile = createCollisionProfile();
    }

    private void refreshModelBounds() {
        playerSpatial.updateModelBound();
        playerSpatial.updateGeometricState();
        updateModelBound();
        updateGeometricState();
    }

    private PlayerCollisionProfile createCollisionProfile() {
        Spatial sourceSpatial = collisionSpatial == null ? playerSpatial : collisionSpatial;
        BoundingBox bounds = boundsOf(sourceSpatial);
        if (bounds == null) {
            return fallbackCollisionProfile(sourceSpatial.getName());
        }

        float modelRadius = Math.max(bounds.getXExtent(), bounds.getZExtent());
        float modelHeight = bounds.getYExtent() * 2f;
        float radius = explicitOrModel(playerOptions.getCollisionRadiusOverride(), modelRadius, playerOptions.getCollisionRadiusScale(), playerOptions.getMinCollisionRadius());
        float height = explicitOrModel(playerOptions.getCollisionHeightOverride(), modelHeight, playerOptions.getCollisionHeightScale(), playerOptions.getMinCollisionHeight());
        Vector3f center = bounds.getCenter().clone();
        Vector3f extent = new Vector3f(bounds.getXExtent(), bounds.getYExtent(), bounds.getZExtent());
        return new PlayerCollisionProfile(sourceName(sourceSpatial), radius, height, playerOptions.getMass(), center, extent);
    }

    private PlayerCollisionProfile fallbackCollisionProfile(String source) {
        float radius = Math.max(playerOptions.getMinCollisionRadius(), playerOptions.getCollisionRadiusOverride() > 0f ? playerOptions.getCollisionRadiusOverride() : 0.35f);
        float height = Math.max(playerOptions.getMinCollisionHeight(), playerOptions.getCollisionHeightOverride() > 0f ? playerOptions.getCollisionHeightOverride() : 1.8f);
        return new PlayerCollisionProfile(source == null ? "fallback" : source, radius, height, playerOptions.getMass(), Vector3f.ZERO, new Vector3f(radius, height * 0.5f, radius));
    }

    private float explicitOrModel(float explicitValue, float modelValue, float modelScale, float minimum) {
        if (explicitValue > 0f) {
            return Math.max(minimum, explicitValue);
        }
        return Math.max(minimum, modelValue * modelScale);
    }

    private String sourceName(Spatial sourceSpatial) {
        if (collisionSpatial == null) {
            return "model-bounds:" + nullSafeName(sourceSpatial);
        }
        return "collision-node:" + nullSafeName(sourceSpatial);
    }

    private String nullSafeName(Spatial spatial) {
        return spatial == null || spatial.getName() == null ? "unnamed" : spatial.getName();
    }

    private BoundingBox boundsOf(Spatial spatial) {
        spatial.updateModelBound();
        spatial.updateGeometricState();
        BoundingVolume bound = spatial.getWorldBound();
        return bound instanceof BoundingBox box ? box : null;
    }

    private Geometry findFirstGeometry(Spatial spatial) {
        if (spatial instanceof Geometry geometry) {
            return geometry;
        }
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                Geometry geometry = findFirstGeometry(child);
                if (geometry != null) {
                    return geometry;
                }
            }
        }
        return null;
    }

    private Spatial findNamedSpatial(Spatial spatial, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        if (name.equals(spatial.getName())) {
            return spatial;
        }
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                Spatial found = findNamedSpatial(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public void setCullHint(Spatial.CullHint cullHint) {
        playerSpatial.setCullHint(cullHint);
    }

    public void setShadowMode(RenderQueue.ShadowMode shadowMode) {
        playerSpatial.setShadowMode(shadowMode);
    }

    public void setVisualVisible(boolean visible, Spatial.CullHint visibleCullHint) {
        playerSpatial.setCullHint(visible ? visibleCullHint : Spatial.CullHint.Always);
    }

    public Spatial getPlayerSpatial() { return playerSpatial; }
    public Geometry getPlayerGeometry() { return playerGeometry; }
    public Spatial getCollisionSpatial() { return collisionSpatial; }
    public PlayerCollisionProfile getCollisionProfile() { return collisionProfile; }
}
