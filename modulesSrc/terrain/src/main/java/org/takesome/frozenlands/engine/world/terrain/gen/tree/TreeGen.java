package org.takesome.frozenlands.engine.world.terrain.gen.tree;

import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.gameplay.GameplayUserData;
import org.takesome.frozenlands.engine.utils.Utils;
import org.takesome.frozenlands.engine.world.terrain.TerrainRuntimeSettings;
import org.takesome.frozenlands.engine.world.terrain.TerrainPlacementGroup;

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
    private final List<TerrainPlacementGroup> placementGroups;
    private final TerrainAssetCollisionFactory collisionFactory;
    private final TerrainPlacementSolver placementSolver;
    private final Map<String, List<Spatial>> variantsByGroup = new LinkedHashMap<>();

    public TreeGen(EngineContext kernelInterface) {
        this.kernelInterface = kernelInterface;
        this.smoothWorldGen = resolveSmoothWorldGen();
        this.settings = new TerrainRuntimeSettings();
        this.placementGroups = settings.placementGroups();
        this.collisionFactory = new TerrainAssetCollisionFactory(kernelInterface);
        this.placementSolver = new TerrainPlacementSolver(kernelInterface);
        initializeAssetModels();
    }

    private void initializeAssetModels() {
        variantsByGroup.putAll(
                new TerrainAssetVariantLoader(kernelInterface)
                        .loadVariants(placementGroups)
        );
        if (variantsByGroup.isEmpty()) {
            throw new IllegalStateException(
                    "No terrain placement asset variants loaded from placement groups: "
                            + placementGroups
            );
        }
    }

    private Spatial randomVariant(TerrainPlacementGroup group) {
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

    public List<Spatial> setupTrees() {
        List<Spatial> result = new ArrayList<>();
        for (TerrainPlacementGroup group : placementGroups) {
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

    private Spatial setupAsset(TerrainPlacementGroup group) {
        Spatial asset = randomVariant(group);
        float scaleFactor = Utils.getRandomNumberInRange(group.scaleMin(), group.scaleMax());
        asset.scale(scaleFactor, scaleFactor, scaleFactor);
        asset.setUserData(GROUP_ID_USER_DATA, group.id());
        markGameplayModel(asset, group, scaleFactor);
        return asset;
    }

    private void markGameplayModel(Spatial asset, TerrainPlacementGroup group, float scaleFactor) {
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
            for (TerrainPlacementGroup group : placementGroups) {
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

    private int randomCount(TerrainPlacementGroup group) {
        if (group.maxPerTile() <= group.minPerTile()) {
            return group.minPerTile();
        }
        return (int) Utils.getRandomNumberInRange(group.minPerTile(), group.maxPerTile() + 1);
    }

    private void placeAndAttachAsset(TerrainQuad quad, List<Spatial> quadAssets, TerrainPlacementGroup group) {
        if (quad.getParent() == null) {
            return;
        }
        Spatial asset = setupAsset(group);
        if (!placementSolver.placeOnQuad(quad, asset, quadAssets, group)) {
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
        collisionFactory.ensureCollision(assetNode, groupFor(assetNode));
    }

    private TerrainPlacementGroup groupFor(Spatial assetNode) {
        Object groupId = assetNode.getUserData(GROUP_ID_USER_DATA);
        if (groupId != null) {
            for (TerrainPlacementGroup group : placementGroups) {
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

}
