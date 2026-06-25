package org.takesome.frozenlands.engine.world.terrain.gen.tree;

import static org.takesome.frozenlands.engine.config.Constants.RAY_DOWN;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
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
import java.util.ArrayList;
import java.util.List;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.config.Constants;
import org.takesome.frozenlands.engine.utils.Utils;

public class TreeGen {
    private static final String QUAD_FOREST_USER_DATA = "quadForest";
    private static final int MIN_TREES_PER_TILE = 80;
    private static final int MAX_TREES_PER_TILE = 160;
    private static final int MAX_PLACEMENT_ATTEMPTS = 8;
    private static final float RAY_START_HEIGHT = 800f;
    private static final List<String> TREE_MODEL_PATHS = List.of(
            "Models/deadFirTrees/deadFirTrees.gltf",
            "Models/deadTrees/deadTrees.gltf"
    );

    private final EngineContext kernelInterface;
    private final List<Spatial> treeVariants = new ArrayList<>();

    public TreeGen(EngineContext kernelInterface) {
        this.kernelInterface = kernelInterface;
        this.initializeTreeModel();
    }

    private void initializeTreeModel() {
        for (String treeModelPath : TREE_MODEL_PATHS) {
            loadTreePackVariants(treeModelPath);
        }
        if (treeVariants.isEmpty()) {
            throw new IllegalStateException("No tree variants loaded from: " + TREE_MODEL_PATHS);
        }
        kernelInterface.getLogger().info(
                "Tree variants loaded: packs={} variants={}",
                TREE_MODEL_PATHS,
                treeVariants.size()
        );
    }

    private void loadTreePackVariants(String treeModelPath) {
        int before = treeVariants.size();
        Spatial treePack = kernelInterface.getAssetManager().loadModel(treeModelPath);
        treePack.setShadowMode(RenderQueue.ShadowMode.Cast);
        Node variantParent = findVariantParent(treePack);
        if (variantParent == null) {
            treeVariants.add(centeredVariant(treePack.clone(), treeVariants.size(), treeModelPath, "fallback-pack"));
            kernelInterface.getLogger().warn("Tree model variant parent not found; using whole pack: {}", treeModelPath);
            return;
        }

        List<Spatial> children = List.copyOf(variantParent.getChildren());
        for (Spatial child : children) {
            if (!containsGeometry(child)) {
                continue;
            }
            Spatial variantPack = treePack.clone();
            Node clonedParent = findNodeByName(variantPack, variantParent.getName());
            if (clonedParent == null) {
                continue;
            }
            keepOnlyChild(clonedParent, child.getName());
            treeVariants.add(centeredVariant(variantPack, treeVariants.size(), treeModelPath, child.getName()));
        }

        if (treeVariants.size() == before) {
            treeVariants.add(centeredVariant(treePack.clone(), treeVariants.size(), treeModelPath, "fallback-pack"));
        }
        kernelInterface.getLogger().info(
                "Tree pack loaded as variants: path={} variantsAdded={} totalVariants={}",
                treeModelPath,
                treeVariants.size() - before,
                treeVariants.size()
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

    private Spatial centeredVariant(Spatial variant, int index, String treeModelPath, String sourceName) {
        Node wrapper = new Node("deadTreeVariant_" + index + "_" + safeName(treeModelPath) + "_" + safeName(sourceName));
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

    private Spatial randomTreeVariant() {
        if (treeVariants.isEmpty()) {
            throw new IllegalStateException("Tree variants were not initialized");
        }
        int index = (int) Utils.getRandomNumberInRange(0, treeVariants.size());
        if (index >= treeVariants.size()) {
            index = treeVariants.size() - 1;
        }
        return treeVariants.get(index).clone();
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
            Spatial treeModelCustom = randomTreeVariant();
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
