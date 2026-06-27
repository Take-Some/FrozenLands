package org.takesome.frozenlands.engine.world.terrain.gen.tree;

import static org.takesome.frozenlands.engine.config.Constants.RAY_DOWN;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.config.Constants;
import org.takesome.frozenlands.engine.gameplay.GameplayUserData;
import org.takesome.frozenlands.engine.utils.Utils;
import org.takesome.frozenlands.engine.world.terrain.TerrainPlacementGroup;

import java.util.List;

final class TerrainPlacementSolver {
    static final String FOOTPRINT_RADIUS_USER_DATA
            = "frozenlands.terrain.asset.footprintRadius";

    private final EngineContext context;

    TerrainPlacementSolver(EngineContext context) {
        this.context = context;
    }

    boolean placeOnQuad(
            TerrainQuad quad,
            Spatial asset,
            List<Spatial> quadAssets,
            TerrainPlacementGroup group
    ) {
        BoundingBox quadBounds = getQuadBounds(quad);
        if (quadBounds == null) {
            context.getLogger().warn(
                    "Cannot place terrain assets: TerrainQuad has no world bounds: {}",
                    quad.getName()
            );
            return false;
        }

        float footprintRadius = footprintRadius(asset, group);
        float margin = footprintRadius + group.footprintPadding() + group.edgePadding();
        Vector3f center = quadBounds.getCenter();
        float minX = center.x - quadBounds.getXExtent() + margin;
        float maxX = center.x + quadBounds.getXExtent() - margin;
        float minZ = center.z - quadBounds.getZExtent() + margin;
        float maxZ = center.z + quadBounds.getZExtent() - margin;
        if (minX >= maxX || minZ >= maxZ) {
            context.getLogger().debug(
                    "Cannot place terrain asset: group={} footprint={} does not fit quad={}",
                    group.id(),
                    footprintRadius,
                    quad.getName()
            );
            return false;
        }

        float rayStartY = center.y + Math.max(quadBounds.getYExtent(), 1f)
                + group.rayStartHeight();
        for (int attempt = 0; attempt < group.placementAttempts(); attempt++) {
            float x = Utils.getRandomNumberInRange(minX, maxX);
            float z = Utils.getRandomNumberInRange(minZ, maxZ);
            CollisionResult centerHit = raycastTerrain(quad, x, z, rayStartY);
            if (centerHit == null
                    || centerHit.getContactPoint().y <= Constants.WATER_LEVEL_HEIGHT) {
                continue;
            }

            float yaw = Utils.getRandomNumberInRange(group.yawMinDeg(), group.yawMaxDeg())
                    * FastMath.DEG_TO_RAD;
            asset.setLocalTranslation(centerHit.getContactPoint());
            asset.setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));
            refreshBounds(asset);

            BoundingBox assetBounds = boundsOf(asset);
            if (assetBounds == null) {
                continue;
            }
            footprintRadius = footprintRadius(assetBounds, group);
            if (!fitsInsideQuadBounds(quadBounds, assetBounds, group.edgePadding())) {
                continue;
            }
            if (!surfaceLooksClean(
                    quad,
                    rayStartY,
                    centerHit.getContactPoint(),
                    footprintRadius,
                    group
            )) {
                continue;
            }
            if (overlapsExisting(quadAssets, asset, footprintRadius, group.minSpacing(), group)) {
                continue;
            }

            asset.setUserData(FOOTPRINT_RADIUS_USER_DATA, footprintRadius);
            return true;
        }

        return false;
    }

    private CollisionResult raycastTerrain(TerrainQuad quad, float x, float z, float rayStartY) {
        CollisionResults results = new CollisionResults();
        quad.collideWith(new Ray(new Vector3f(x, rayStartY, z), RAY_DOWN), results);
        return results.getClosestCollision();
    }

    private boolean fitsInsideQuadBounds(
            BoundingBox quadBounds,
            BoundingBox assetBounds,
            float edgePadding
    ) {
        Vector3f quadCenter = quadBounds.getCenter();
        Vector3f assetCenter = assetBounds.getCenter();
        float quadMinX = quadCenter.x - quadBounds.getXExtent() + edgePadding;
        float quadMaxX = quadCenter.x + quadBounds.getXExtent() - edgePadding;
        float quadMinZ = quadCenter.z - quadBounds.getZExtent() + edgePadding;
        float quadMaxZ = quadCenter.z + quadBounds.getZExtent() - edgePadding;
        float assetMinX = assetCenter.x - assetBounds.getXExtent();
        float assetMaxX = assetCenter.x + assetBounds.getXExtent();
        float assetMinZ = assetCenter.z - assetBounds.getZExtent();
        float assetMaxZ = assetCenter.z + assetBounds.getZExtent();
        return assetMinX >= quadMinX
                && assetMaxX <= quadMaxX
                && assetMinZ >= quadMinZ
                && assetMaxZ <= quadMaxZ;
    }

    private boolean surfaceLooksClean(
            TerrainQuad quad,
            float rayStartY,
            Vector3f centerPoint,
            float footprintRadius,
            TerrainPlacementGroup group
    ) {
        float sampleRadius = Math.max(0.1f, footprintRadius + group.footprintPadding());
        float diagonal = sampleRadius * 0.70710677f;
        float[][] offsets = {
                {0f, 0f},
                {sampleRadius, 0f},
                {-sampleRadius, 0f},
                {0f, sampleRadius},
                {0f, -sampleRadius},
                {diagonal, diagonal},
                {-diagonal, diagonal},
                {diagonal, -diagonal},
                {-diagonal, -diagonal}
        };

        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (float[] offset : offsets) {
            CollisionResult hit = raycastTerrain(
                    quad,
                    centerPoint.x + offset[0],
                    centerPoint.z + offset[1],
                    rayStartY
            );
            if (hit == null || hit.getContactPoint().y <= Constants.WATER_LEVEL_HEIGHT) {
                return false;
            }
            float y = hit.getContactPoint().y;
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return maxY - minY <= group.maxSurfaceDelta();
    }

    private boolean overlapsExisting(
            List<Spatial> quadAssets,
            Spatial candidate,
            float candidateRadius,
            float minSpacing,
            TerrainPlacementGroup fallbackGroup
    ) {
        Vector3f candidateLocation = candidate.getWorldTranslation();
        for (Spatial existing : quadAssets) {
            if (existing == null || existing == candidate) {
                continue;
            }
            if (Boolean.TRUE.equals(existing.getUserData(GameplayUserData.GRINDABLE_DESTROYED))) {
                continue;
            }
            float existingRadius = userDataFloat(
                    existing,
                    FOOTPRINT_RADIUS_USER_DATA,
                    footprintRadius(existing, fallbackGroup)
            );
            float required = candidateRadius + existingRadius + minSpacing;
            float dx = candidateLocation.x - existing.getWorldTranslation().x;
            float dz = candidateLocation.z - existing.getWorldTranslation().z;
            if (dx * dx + dz * dz < required * required) {
                return true;
            }
        }
        return false;
    }

    private float footprintRadius(Spatial spatial, TerrainPlacementGroup group) {
        BoundingBox bounds = boundsOf(spatial);
        return bounds == null
                ? Math.max(0.5f, group == null ? 0.5f : group.footprintPadding())
                : footprintRadius(bounds, group);
    }

    private float footprintRadius(BoundingBox bounds, TerrainPlacementGroup group) {
        float fromBounds = (float) Math.sqrt(
                bounds.getXExtent() * bounds.getXExtent()
                        + bounds.getZExtent() * bounds.getZExtent()
        );
        float padding = group == null ? 0f : group.footprintPadding();
        return Math.max(0.25f, fromBounds + padding);
    }

    private float userDataFloat(Spatial spatial, String key, float fallback) {
        Object value = spatial.getUserData(key);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private BoundingBox getQuadBounds(TerrainQuad quad) {
        refreshBounds(quad);
        if (quad.getWorldBound() instanceof BoundingBox box) {
            return box;
        }
        return null;
    }

    private BoundingBox boundsOf(Spatial spatial) {
        refreshBounds(spatial);
        BoundingVolume bound = spatial.getWorldBound();
        return bound instanceof BoundingBox box ? box : null;
    }

    private void refreshBounds(Spatial spatial) {
        spatial.updateModelBound();
        spatial.updateGeometricState();
    }
}
