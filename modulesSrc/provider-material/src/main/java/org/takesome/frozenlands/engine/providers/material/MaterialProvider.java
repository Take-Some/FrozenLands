package org.takesome.frozenlands.engine.providers.material;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.providers.EngineProvider;
import org.takesome.frozenlands.engine.providers.EngineProviders;
import org.takesome.frozenlands.engine.providers.ProviderCommand;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class MaterialProvider extends MaterialAbstract implements EngineProvider {
    private static final ModuleIndexCatalog MODULE_INDEX = ModuleIndexCatalog.defaultCatalog();
    private static final String DEFAULT_MATERIALS_CONFIG = MODULE_INDEX.configPath(EngineProviders.MATERIAL_PROVIDER, "materials");

    private final Map<String, Material> materials = new LinkedHashMap<>();
    private final Map<String, ProviderCommand> commands = new LinkedHashMap<>();
    private Map<String, Object> matData;

    public MaterialProvider(EngineContext engineContext) {
        setAssetManager(engineContext);
        registerCommands();
    }

    @Override
    public String id() {
        return EngineProviders.MATERIAL_PROVIDER;
    }

    @Override
    public void register(EngineContext context) {
        loadMaterials(DEFAULT_MATERIALS_CONFIG);
    }

    @Override
    public Map<String, ProviderCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    @Override
    public Map<String, Object> luaDescriptor() {
        Map<String, Object> descriptor = EngineProvider.super.luaDescriptor();
        descriptor.put("materials", List.copyOf(materials.keySet()));
        return descriptor;
    }

    @Override
    public void loadMaterials(String path) {
        materials.clear();
        getEngineContext().getLogger().info("Adding materials");
        JsonNode materialsNode = MODULE_INDEX.readJsonNode(path);
        materialsNode.forEach(material -> {
            String[] matArr = material.asText().split("#");
            String mat = matArr[0];
            String type = matArr[1];
            String materialId = mat + '#' + type;
            getEngineContext().getLogger().info("  - Adding '" + mat + "' material of type " + type);
            materials.put(materialId, createMat(mat, type));
        });

        Map<String, Object> event = result("count", materials.size());
        event.put("path", path);
        getEngineContext().getProviderRegistry().publishEvent("provider.material.loaded", event);
        getEngineContext().getLogger().info("Finished adding materials, total matAmount: " + materials.size());
    }

    @Override
    public Material createMat(String dir, String type) {
        String baseDir = "textures/" + dir + '/';
        matData = MODULE_INDEX.readAssetJsonMap(getEngineContext().getAssetManager(),
                baseDir + "matOpt/" + type + ".json");
        initMaterial(String.valueOf(matData.get("matDef")));

        AtomicInteger textNum = new AtomicInteger();
        AtomicInteger varNum = new AtomicInteger();
        handleTextures((mapName, textureInstanceMap) -> {
            TextureWrap wrapType = TextureWrap.valueOf((String) textureInstanceMap.get("wrap"));
            Texture thisTexture = getEngineContext().getAssetManager()
                    .loadTexture(baseDir + "textures/" + textureInstanceMap.get("texture"));
            wrapType(wrapType, thisTexture);
            getMaterial().setTexture(mapName, thisTexture);
            textNum.incrementAndGet();
        });

        handleVars((cfgTitle, value) -> {
            inputType(cfgTitle, value);
            varNum.incrementAndGet();
        });
        getEngineContext().getLogger().info("    - " + dir + '#' + type + " has "
                + textNum + " textures and " + varNum + " vars");

        return getMaterial();
    }

    public Material getMaterial(String mat) {
        return materials.get(mat);
    }

    public List<String> getMaterialIds() {
        return List.copyOf(materials.keySet());
    }

    private void registerCommands() {
        commands.put("load", ProviderCommand.of("load", "Load material declarations from a resource path", args -> {
            String path = stringArg(args, "path", DEFAULT_MATERIALS_CONFIG);
            loadMaterials(path);
            return result("count", materials.size());
        }));
        commands.put("list", ProviderCommand.of("list", "List registered material ids", args -> result("materials", getMaterialIds())));
        commands.put("get", ProviderCommand.of("get", "Check whether a material id exists", args -> {
            String materialId = stringArg(args, "id", "");
            Map<String, Object> result = result("id", materialId);
            result.put("exists", materials.containsKey(materialId));
            return result;
        }));
    }

    private void wrapType(TextureWrap wrapType, Texture thisTexture) {
        switch (wrapType) {
            case REPEAT -> thisTexture.setWrap(Texture.WrapMode.Repeat);
            case MIRRORED_REPEAT -> thisTexture.setWrap(Texture.WrapMode.MirroredRepeat);
            case EDGE_CLAMP -> thisTexture.setWrap(Texture.WrapMode.EdgeClamp);
            case NONE -> { }
        }
    }

    private void inputType(String cfgTitle, Map<String, Object> value) {
        VarType inputType = VarType.valueOf(((String) value.get("type")).toUpperCase());
        switch (inputType) {
            case FLOAT -> setMaterialFloat(cfgTitle, ((Number) value.get("value")).floatValue());
            case BOOLEAN -> setMaterialBoolean(cfgTitle, (Boolean) value.get("value"));
            case COLOR -> setMaterialColor(cfgTitle, parseColor((String) value.get("value")));
            case VECTOR -> setMaterialVector(cfgTitle, (String) value.get("value"));
        }
    }

    private void handleTextures(BiConsumer<String, Map<String, Object>> consumer) {
        LinkedHashMap<String, Map<String, Object>> texturesMap =
                (LinkedHashMap<String, Map<String, Object>>) matData.get("textures");
        texturesMap.forEach(consumer::accept);
    }

    private void handleVars(BiConsumer<String, Map<String, Object>> consumer) {
        LinkedHashMap<String, Map<String, Object>> varsMap =
                (LinkedHashMap<String, Map<String, Object>>) matData.get("vars");
        varsMap.forEach(consumer::accept);
    }

    private ColorRGBA parseColor(String colorStr) {
        String[] rgba = colorStr.split(",");
        return new ColorRGBA(Float.parseFloat(rgba[0]), Float.parseFloat(rgba[1]),
                Float.parseFloat(rgba[2]), Float.parseFloat(rgba[3]));
    }

    private Map<String, Object> readMatConfig(InputStream is) {
        if (is == null) {
            throw new IllegalArgumentException("Material config resource was not found");
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<>() { };
            return mapper.readValue(is, typeRef);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read material config", e);
        }
    }

    private String stringArg(Map<String, Object> args, String name, String fallback) {
        Object value = args == null ? null : args.get(name);
        return value == null ? fallback : String.valueOf(value);
    }

    private Map<String, Object> result(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put(key, value);
        return result;
    }

    private enum VarType {
        FLOAT,
        VECTOR,
        BOOLEAN,
        COLOR
    }

    @FunctionalInterface
    private interface BiConsumer<T, U> {
        void accept(T t, U u);
    }
}
