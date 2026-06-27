package org.takesome.frozenlands.engine.world.terrain.gen.tree;

import static org.takesome.frozenlands.engine.config.Constants.RAY_DOWN;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
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
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.config.Constants;
import org.takesome.frozenlands.engine.gameplay.GameplayUserData;
import org.takesome.frozenlands.engine.gameplay.GrindableCollisionProxyControl;
import org.takesome.frozenlands.engine.utils.Utils;
import org.takesome.frozenlands.engine.world.terrain.TerrainRuntimeSettings;
import org.takesome.frozenlands.engine.world.terrain.TerrainRuntimeSettings.PlacementGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TreeGen {
    public static final String QUAD_ASSET_USER_DATA = "quadTerrainAssets";
    public static final String LEGACY_QUAD_FOREST_USER_DATA = "quadForest";

    private static final String GROUP_ID_USER_DATA = "frozenlands.terrain.asset.group";
    private static final String FOOTPRINT_RADIUS_USER_DATA = "frozenlands.terrain.asset.footprintRadius";

    private final EngineContext kernelInterface;
    private final SmoothWorldGenState smoothWorldGen;
    private final TerrainRuntimeSettings settings;
    private final List<PlacementGroup> placementGroups;
    private final Map<String, List<Spatial>> variantsByGroup = new LinkedHashMap<>();

    public TreeGen(EngineContext kernelInterface) {
        this.kernelInterface = kernelInterface;
        this.smoothWorldGen = resolveSmoothWorldGen();
        this.settings = new TerrainRuntimeSettings();
        this.placementGroups = settings.placementGroups();
        initializeAssetModels();
    }

    private void initializeAssetModels() {
        for (PlacementGroup group : placementGroups) {
            List<Spatial> variants = new ArrayList<>();
            for (String modelPath : group.modelPaths()) {
                loadAssetPackVariants(group, modelPath, variants);
            }
            if (!variants.isEmpty()) {
                variantsByGroup.put(group.id(), List.copyOf(variants));
            }
            kernelInterface.getLogger().info(
                    "Terrain asset variants loaded: group={} kind={} packs={} variants={}",
                    group.id(),
                    group.kind(),
                    group.modelPaths(),
                    variants.size()
            );
        }
        if (variantsByGroup.isEmpty()) {
            throw new IllegalStateException("No terrain placement asset variants loaded from placement groups: " + placementGroups);
        }
    }

    private void loadAssetPackVariants(PlacementGroup group, String modelPath, List<Spatial> target) {
        int before = target.size();
        Spatial assetPack = kernelInterface.getAssetManager().loadModel(modelPath);
        assetPack.setShadowMode(RenderQueue.ShadowMode.Cast);
        Node variantParent = findVariantParent(assetPack);
        if (variantParent == null) {
            target.add(centeredVariant(assetPack.clone(), group, target.size(), modelPath, "fallback-pack"));
            kernelInterface.getLogger().warn("Terrain asset variant parent not found; using whole pack: group={} path={}", group.id(), modelPath);
            return;
        }

        List<Spatial> children = List.copyOf(variantParent.getChildren());
        for (Spatial child : children) {
            if (!containsGeometry(child)) {
                continue;
            }
            Spatial variantPack = assetPack.clone();
            Node clonedParent = findNodeByName(variantPack, variantParent.getName());
            if (clonedParent == null) {
                continue;
            }
            keepOnlyChild(clonedParent, child.getName());
            target.add(centeredVariant(variantPack, group, target.size(), modelPath, child.getName()));
        }

        if (target.size() == before) {
            target.add(centeredVariant(assetPack.clone(), group, target.size(), modelPath, "fallback-pack"));
        }
        kernelInterface.getLogger().info(
                "Terrain asset pack loaded as variants: group={} path={} variantsAdded={} totalVariants={}",
                group.id(),
                modelPath,
                target.size() - before,
                target.size()
        );
    }

    private Node findVariantParent(Spatial spatial) {
        if (!(spatial instanceof Node node)) {
            return null;
        }
        long renderableChildren = node.getChildren().stream().filter(this::containsGeometry).count();
        if (renderableChildren > 1) {
            return node;
        }
        for (Spatial child : node.getChildren()) {
            Node found = findVariantParent(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private boolean containsGeometry(Spatial spatial) {
        if (spatial instanceof Geometry) {
            return true;
        }
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                if (containsGeometry(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Node findNodeByName(Spatial spatial, String name) {
        if (spatial instanceof Node node) {
            if (name == null ? node.getName() == null : name.equals(node.getName())) {
                return node;
            }
            for (Spatial child : node.getChildren()) {
                Node found = findNodeByName(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void keepOnlyChild(Node parent, String childName) {
        for (Spatial child : List.copyOf(parent.getChildren())) {
            if (childName == null ? child.getName() != null : !childName.equals(child.getName())) {
                child.removeFromParent();
            }
        }
    }

    private Spatial centeredVariant(Spatial variant, PlacementGroup group, int index, String modelPath, String sourceName) {
        Node wrapper = new Node("terrainAssetVariant_" + index + "_" + safeName(group.id()) + "_" + safeName(modelPath) + "_" + safeName(sourceName));
        wrapper.attachChild(variant);
        wrapper.setShadowMode(RenderQueue.ShadowMode.Cast);
        refreshBounds(wrapper);

        BoundingBox bounds = boundsOf(wrapper);
        if (bounds != null) {
            Vector3f center = bounds.getCenter();
            float bottom = center.y - bounds.getYExtent();
            variant.move(-center.x, -bottom, -center.z);
            refreshBounds(wrapper);
        }
        return wrapper;
    }

    private String safeName(String name) {
        return name == null || name.isBlank() ? "unnamed" : name.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private void refreshBounds(Spatial spatial) {
        spatial.updateModelBound();
        spatial.updateGeometricState();
    }

    private BoundingBox boundsOf(Spatial spatial) {
        refreshBounds(spatial);
        BoundingVolume bound = spatial.getWorldBound();
        return bound instanceof BoundingBox box ? box : null;
    }

    private Spatial randomVariant(PlacementGroup group) {
        List<Spatial> variants = variantsByGroup.get(group.id());
        if (variants == null || variants.isEmpty()) {
            throw new IllegalStateException("Terrain placement group has no loaded variants: " + group.id());
        }
        int index = (int) Utils.getRandomNumberInRange(0, variants.size());
        if (index >= variants.size()) {
            index = variants.size() - 1;
        }
        return variants.get(index).clone();
    }

    public void enqueueSmooth(String label, Runnable operation) {
        smoothWorldGen.enqueue(label, operation);
    }

    private RigidBodyControl createCollisionControl(Spatial spatial, PlacementGroup group) {
        removeExistingCollision(spatial);
        if (group.collisionMesh()) {
            return createMeshCollisionControl(spatial);
        }
        return createTrunkProxyCollisionControl(spatial, group);
    }

    private void removeExistingCollision(Spatial spatial) {
        RigidBodyControl existingControl = spatial.getControl(RigidBodyControl.class);
        if (existingControl != null) {
            kernelInterface.getBulletAppState().getPhysicsSpace().remove(existingControl);
            spatial.removeControl(RigidBodyControl.class);
        }
        GrindableCollisionProxyControl proxyControl = spatial.getControl(GrindableCollisionProxyControl.class);
        if (proxyControl != null) {
            proxyControl.detachProxy();
            spatial.removeControl(proxyControl);
        }
    }

    private RigidBodyControl createMeshCollisionControl(Spatial spatial) {
        CollisionShape shape = CollisionShapeFactory.createMeshShape(spatial);
        RigidBodyControl control = new RigidBodyControl(shape, 0);
        spatial.addControl(control);
        kernelInterface.getBulletAppState().getPhysicsSpace().add(control);
        return control;
    }

    private RigidBodyControl createTrunkProxyCollisionControl(Spatial spatial, PlacementGroup group) {
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
        kernelInterface.getRootNode().attachChild(proxyNode);
        kernelInterface.getBulletAppState().getPhysicsSpace().add(control);
        spatial.addControl(new GrindableCollisionProxyControl(proxyNode, control, kernelInterface.getBulletAppState().getPhysicsSpace()));
        return control;
    }

    public List<Spatial> setupTrees() {
        List<Spatial> result = new ArrayList<>();
        for (PlacementGroup group : placementGroups) {
            if (!group.enabled()) {
                continue;
            }
            int count = randomCount(group);
            for (int i = 0; i < count; i++) {
                result.add(setupAsset(group));
            }
        }
        return result;
    }

    private Spatial setupAsset(PlacementGroup group) {
        Spatial asset = randomVariant(group);
        float scaleFactor = Utils.getRandomNumberInRange(group.scaleMin(), group.scaleMax());
        asset.scale(scaleFactor, scaleFactor, scaleFactor);
        asset.setUserData(GROUP_ID_USER_DATA, group.id());
        markGameplayModel(asset, group, scaleFactor);
        return asset;
    }

    private void markGameplayModel(Spatial asset, PlacementGroup group, float scaleFactor) {
        if (!group.grindable()) {
            return;
        }
        float maxHealth = group.baseHealth() + (scaleFactor * group.healthPerScale());
        asset.setUserData(GameplayUserData.GRINDABLE, true);
        asset.setUserData(GameplayUserData.GRINDABLE_MODEL, true);
        asset.setUserData(GameplayUserData.GRINDABLE_DESTROYED, false);
        asset.setUserData(GameplayUserData.GRINDABLE_ID, System.identityHashCode(asset));
        asset.setUserData(GameplayUserData.GRINDABLE_KIND, group.gameplayKindCode() == 0 ? GameplayUserData.KIND_TREE : group.gameplayKindCode());
        asset.setUserData(GameplayUserData.GRINDABLE_MAX_HEALTH, maxHealth);
        asset.setUserData(GameplayUserData.GRINDABLE_HEALTH, maxHealth);
    }

    public void positionTrees(TerrainQuad quad) {
        List<Spatial> quadAssets = quad.getUserData(QUAD_ASSET_USER_DATA);
        if (quadAssets == null) {
            quadAssets = new ArrayList<>();
            quad.setUserData(QUAD_ASSET_USER_DATA, quadAssets);
            quad.setUserData(LEGACY_QUAD_FOREST_USER_DATA, quadAssets);
            List<Spatial> pendingAssets = quadAssets;
            for (PlacementGroup group : placementGroups) {
                if (!group.enabled() || !variantsByGroup.containsKey(group.id())) {
                    continue;
                }
                int count = randomCount(group);
                for (int i = 0; i < count; i++) {
                    smoothWorldGen.enqueue("terrain-asset-place:" + group.id() + ":" + quad.getName(), () -> placeAndAttachAsset(quad, pendingAssets, group));
                }
            }
            return;
        }

        for (Spatial assetNode : quadAssets) {
            smoothWorldGen.enqueue("terrain-asset-reattach:" + quad.getName(), () -> attachExistingAsset(assetNode));
        }
    }

    private int randomCount(PlacementGroup group) {
        if (group.maxPerTile() <= group.minPerTile()) {
            return group.minPerTile();
        }
        return (int) Utils.getRandomNumberInRange(group.minPerTile(), group.maxPerTile() + 1);
    }

    private void placeAndAttachAsset(TerrainQuad quad, List<Spatial> quadAssets, PlacementGroup group) {
        if (quad.getParent() == null) {
            return;
        }
        Spatial asset = setupAsset(group);
        if (!placeAssetOnQuad(quad, asset, quadAssets, group)) {
            return;
        }
        kernelInterface.getRootNode().attachChild(asset);
        ensureAssetCollision(asset);
        quadAssets.add(asset);
        kernelInterface.getLogger().debug("Attached terrain asset group={} id={} at={}", group.id(), asset.hashCode(), asset.getLocalTranslation());
    }

    private void attachExistingAsset(Spatial assetNode) {
        if (Boolean.TRUE.equals(assetNode.getUserData(GameplayUserData.GRINDABLE_DESTROYED))) {
            return;
        }
        if (assetNode.getParent() == null) {
            kernelInterface.getRootNode().attachChild(assetNode);
        }
        ensureAssetCollision(assetNode);
        kernelInterface.getLogger().debug("Attached terrain asset again {} at={}", assetNode.hashCode(), assetNode.getLocalTranslation());
    }

    private void ensureAssetCollision(Spatial assetNode) {
        if (Boolean.TRUE.equals(assetNode.getUserData(GameplayUserData.GRINDABLE_DESTROYED))) {
            return;
        }
        PlacementGroup group = groupFor(assetNode);
        if (group == null || !group.collisionEnabled()) {
            return;
        }
        if (assetNode.getControl(RigidBodyControl.class) == null
                && assetNode.getControl(GrindableCollisionProxyControl.class) == null) {
            createCollisionControl(assetNode, group);
        }
    }

    private PlacementGroup groupFor(Spatial assetNode) {
        Object groupId = assetNode.getUserData(GROUP_ID_USER_DATA);
        if (groupId != null) {
            for (PlacementGroup group : placementGroups) {
                if (group.id().equals(String.valueOf(groupId))) {
                    return group;
                }
            }
        }
        return placementGroups.isEmpty() ? null : placementGroups.get(0);
    }

    private SmoothWorldGenState resolveSmoothWorldGen() {
        SmoothWorldGenState existing = kernelInterface.serviceOrNull(SmoothWorldGenState.class);
        if (existing != null) {
            return existing;
        }
        SmoothWorldGenState created = new SmoothWorldGenState(kernelInterface);
        kernelInterface.registerService(SmoothWorldGenState.class, created);
        kernelInterface.appStateManager().attach(created);
        return created;
    }

    private boolean placeAssetOnQuad(TerrainQuad quad, Spatial asset, List<Spatial> quadAssets, PlacementGroup group) {
        BoundingBox quadBounds = getQuadBounds(quad);
        if (quadBounds == null) {
            kernelInterface.getLogger().warn("Cannot place terrain assets: TerrainQuad has no world bounds: {}", quad.getName());
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
            kernelInterface.getLogger().debug("Cannot place terrain asset: group={} footprint={} does not fit quad={}", group.id(), footprintRadius, quad.getName());
            return false;
        }

        float rayStartY = center.y + Math.max(quadBounds.getYExtent(), 1f) + group.rayStartHeight();
        for (int attempt = 0; attempt < group.placementAttempts(); attempt++) {
            float x = Utils.getRandomNumberInRange(minX, maxX);
            float z = Utils.getRandomNumberInRange(minZ, maxZ);
            CollisionResult centerHit = raycastTerrain(quad, x, z, rayStartY);
            if (centerHit == null || centerHit.getContactPoint().y <= Constants.WATER_LEVEL_HEIGHT) {
                continue;
            }

            float yaw = Utils.getRandomNumberInRange(group.yawMinDeg(), group.yawMaxDeg()) * FastMath.DEG_TO_RAD;
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
            if (!surfaceLooksClean(quad, rayStartY, centerHit.getContactPoint(), footprintRadius, group)) {
                continue;
            }
            if (overlapsExisting(quadAssets, asset, footprintRadius, group.minSpacing())) {
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

    private boolean fitsInsideQuadBounds(BoundingBox quadBounds, BoundingBox assetBounds, float edgePadding) {
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
        return assetMinX >= quadMinX && assetMaxX <= quadMaxX && assetMinZ >= quadMinZ && assetMaxZ <= quadMaxZ;
    }

    private boolean surfaceLooksClean(TerrainQuad quad, float rayStartY, Vector3f centerPoint, float footprintRadius, PlacementGroup group) {
        float sampleRadius = Math.max(0.1f, footprintRadius + group.footprintPadding());
        float diagonal = sampleRadius * 0.70710677f;
        float[][] offsets = {
                {0f, 0f},
                {sampleRadius, 0f}, {-sampleRadius, 0f}, {0f, sampleRadius}, {0f, -sampleRadius},
                {diagonal, diagonal}, {-diagonal, diagonal}, {diagonal, -diagonal}, {-diagonal, -diagonal}
        };

        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (float[] offset : offsets) {
            CollisionResult hit = raycastTerrain(quad, centerPoint.x + offset[0], centerPoint.z + offset[1], rayStartY);
            if (hit == null || hit.getContactPoint().y <= Constants.WATER_LEVEL_HEIGHT) {
                return false;
            }
            float y = hit.getContactPoint().y;
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return maxY - minY <= group.maxSurfaceDelta();
    }

    private boolean overlapsExisting(List<Spatial> quadAssets, Spatial candidate, float candidateRadius, float minSpacing) {
        Vector3f candidateLocation = candidate.getWorldTranslation();
        for (Spatial existing : quadAssets) {
            if (existing == null || existing == candidate) {
                continue;
            }
            if (Boolean.TRUE.equals(existing.getUserData(GameplayUserData.GRINDABLE_DESTROYED))) {
                continue;
            }
            float existingRadius = userDataFloat(existing, FOOTPRINT_RADIUS_USER_DATA, footprintRadius(existing, groupFor(existing)));
            float required = candidateRadius + existingRadius + minSpacing;
            float dx = candidateLocation.x - existing.getWorldTranslation().x;
            float dz = candidateLocation.z - existing.getWorldTranslation().z;
            if (dx * dx + dz * dz < required * required) {
                return true;
            }
        }
        return false;
    }

    private float footprintRadius(Spatial spatial, PlacementGroup group) {
        BoundingBox bounds = boundsOf(spatial);
        return bounds == null ? Math.max(0.5f, group == null ? 0.5f : group.footprintPadding()) : footprintRadius(bounds, group);
    }

    private float footprintRadius(BoundingBox bounds, PlacementGroup group) {
        float fromBounds = (float) Math.sqrt(bounds.getXExtent() * bounds.getXExtent() + bounds.getZExtent() * bounds.getZExtent());
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
