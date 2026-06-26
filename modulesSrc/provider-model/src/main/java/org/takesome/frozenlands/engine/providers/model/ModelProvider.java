package org.takesome.frozenlands.engine.providers.model;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.providers.EngineProvider;
import org.takesome.frozenlands.engine.providers.EngineProviders;
import org.takesome.frozenlands.engine.providers.ProviderCommand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.takesome.frozenlands.engine.providers.material.MaterialProvider;

public class ModelProvider implements EngineProvider {
    private final EngineContext engineContext;
    private final AssetManager assetManager;
    private final BulletAppState bulletAppState;
    private final Node rootNode;
    private final Map<String, Spatial> modelsMap = new LinkedHashMap<>();
    private final Map<String, ProviderCommand> commands = new LinkedHashMap<>();
    private int sequence = 0;

    public ModelProvider(EngineContext engineContext) {
        this.engineContext = engineContext;
        this.assetManager = engineContext.getAssetManager();
        this.bulletAppState = engineContext.getBulletAppState();
        this.rootNode = engineContext.getRootNode();
        registerCommands();
    }

    @Override
    public String id() {
        return EngineProviders.MODEL_PROVIDER;
    }

    @Override
    public Map<String, ProviderCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    @Override
    public Map<String, Object> luaDescriptor() {
        Map<String, Object> descriptor = EngineProvider.super.luaDescriptor();
        descriptor.put("models", List.copyOf(modelsMap.keySet()));
        return descriptor;
    }

    public Spatial loadModels(String modelPath, String material) {
        return loadModel(modelPath, material, nextId("model"), true, true);
    }

    public Spatial getModel(String id) {
        return modelsMap.get(id);
    }

    public List<String> getModelIds() {
        return List.copyOf(modelsMap.keySet());
    }

    private Spatial loadModel(String modelPath, String material, String id, boolean attach, boolean collision) {
        Spatial model = assetManager.loadModel(modelPath);
        model.setName(id);
        if (material != null && !material.isBlank()) {
            model.setMaterial(engineContext.requireService(MaterialProvider.class).getMaterial(material));
        }
        if (attach) {
            rootNode.attachChild(model);
        }
        if (collision) {
            createCollisionControl(model);
        }
        modelsMap.put(id, model);
        engineContext.getProviderRegistry().publishEvent("provider.model.loaded", Map.of("id", id, "path", modelPath));
        return model;
    }

    private boolean detachModel(String id) {
        Spatial model = modelsMap.remove(id);
        if (model == null) {
            return false;
        }
        RigidBodyControl control = model.getControl(RigidBodyControl.class);
        if (control != null) {
            bulletAppState.getPhysicsSpace().remove(control);
            model.removeControl(RigidBodyControl.class);
        }
        model.removeFromParent();
        engineContext.getProviderRegistry().publishEvent("provider.model.detached", Map.of("id", id));
        return true;
    }

    private RigidBodyControl createCollisionControl(Spatial spatial) {
        CollisionShape shape = CollisionShapeFactory.createMeshShape(spatial);
        RigidBodyControl control = new RigidBodyControl(shape, 0);
        spatial.addControl(control);
        bulletAppState.getPhysicsSpace().add(control);
        return control;
    }

    private void registerCommands() {
        commands.put("load", ProviderCommand.of("load", "Load and optionally attach a model", args -> {
            String path = stringArg(args, "path", "");
            String material = stringArg(args, "material", "");
            String id = stringArg(args, "id", nextId("model"));
            boolean attach = booleanArg(args, "attach", true);
            boolean collision = booleanArg(args, "collision", true);
            loadModel(path, material, id, attach, collision);
            Map<String, Object> result = result("id", id);
            result.put("path", path);
            return result;
        }));
        commands.put("list", ProviderCommand.of("list", "List loaded model ids", args -> result("models", getModelIds())));
        commands.put("detach", ProviderCommand.of("detach", "Detach and forget a loaded model", args -> {
            String id = stringArg(args, "id", "");
            Map<String, Object> result = result("detached", detachModel(id));
            result.put("id", id);
            return result;
        }));
    }

    private String nextId(String prefix) {
        return prefix + '-' + sequence++;
    }

    private String stringArg(Map<String, Object> args, String name, String fallback) {
        Object value = args == null ? null : args.get(name);
        return value == null ? fallback : String.valueOf(value);
    }

    private boolean booleanArg(Map<String, Object> args, String name, boolean fallback) {
        Object value = args == null ? null : args.get(name);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private Map<String, Object> result(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put(key, value);
        return result;
    }
}
