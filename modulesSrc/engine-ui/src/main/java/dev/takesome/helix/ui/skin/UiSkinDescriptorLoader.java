package dev.takesome.helix.ui.skin;

import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.takesome.helix.data.api.JsonDataReader;
import dev.takesome.helix.data.io.DataFiles;
import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads game/editor-owned UI skin descriptors from JSON data files. */
public final class UiSkinDescriptorLoader {
    private static final Logger LOG = EngineLog.logger(UiSkinDescriptorLoader.class);
    private static final int DESCRIPTOR_CACHE_LIMIT = Math.max(0, Integer.getInteger("helix.ui.skin.cache.descriptors", 32));
    private static final Map<String, CachedDescriptorFile> DESCRIPTOR_CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedDescriptorFile> eldest) {
            return DESCRIPTOR_CACHE_LIMIT > 0 && size() > DESCRIPTOR_CACHE_LIMIT;
        }
    };

    private final JsonDataReader reader;

    public UiSkinDescriptorLoader() {
        this(JsonDataReader.defaultReader());
    }

    public UiSkinDescriptorLoader(JsonDataReader reader) {
        this.reader = reader == null ? JsonDataReader.defaultReader() : reader;
    }

    public static void clearRuntimeCaches() {
        synchronized (DESCRIPTOR_CACHE) {
            DESCRIPTOR_CACHE.clear();
        }
    }

    public int loadInto(UiSkinRegistry registry, String path) {
        if (registry == null || path == null || path.isBlank()) return 0;
        String cleanPath = DataFiles.normalize(path.trim());
        CachedDescriptorFile file = loadDescriptorFile(cleanPath);
        for (UiSkinDescriptor descriptor : file.descriptors()) {
            registry.register(descriptor);
        }
        return file.descriptors().size();
    }

    private CachedDescriptorFile loadDescriptorFile(String cleanPath) {
        FileStamp stamp = fileStamp(cleanPath);
        if (stamp.cacheable() && DESCRIPTOR_CACHE_LIMIT > 0) {
            synchronized (DESCRIPTOR_CACHE) {
                CachedDescriptorFile cached = DESCRIPTOR_CACHE.get(cleanPath);
                if (cached != null && cached.stamp().equals(stamp)) return cached;
            }
        }

        CachedDescriptorFile parsed = parseDescriptorFile(cleanPath, stamp);
        if (stamp.cacheable() && DESCRIPTOR_CACHE_LIMIT > 0) {
            synchronized (DESCRIPTOR_CACHE) {
                DESCRIPTOR_CACHE.put(cleanPath, parsed);
            }
        }
        return parsed;
    }

    private CachedDescriptorFile parseDescriptorFile(String cleanPath, FileStamp stamp) {
        JsonObject root = reader.readObject(cleanPath);
        JsonArray skins = firstArray(root, "skins", "items", "entries", "descriptors");
        if (skins == null) {
            LOG.warn("UI skin descriptor file '{}' contains no skin descriptor array", cleanPath);
            return new CachedDescriptorFile(stamp, List.of());
        }

        ArrayList<UiSkinDescriptor> descriptors = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        int index = 0;
        for (JsonElement element : skins) {
            if (element == null || !element.isJsonObject()) {
                LOG.warn("UI skin descriptor file '{}' contains non-object descriptor at index={}; skipped", cleanPath, index);
                index++;
                continue;
            }
            UiSkinDescriptor descriptor = descriptor(element.getAsJsonObject());
            if (!ids.add(descriptor.id())) duplicates.add(descriptor.id());
            if (descriptor.type() == UiSkinType.THREE_SLICE && !descriptor.threeSlice().valid()) {
                LOG.warn("UI skin descriptor '{}' in '{}' has invalid three-slice parts; renderer will fall back where possible", descriptor.id(), cleanPath);
            }
            descriptors.add(descriptor);
            index++;
        }
        if (!duplicates.isEmpty()) {
            LOG.warn("UI skin descriptor file '{}' contains duplicate ids count={} ids={}", cleanPath, duplicates.size(), abbreviate(duplicates));
        }
        LOG.debug("Loaded UI skin descriptors path='{}' count={}", cleanPath, descriptors.size());
        return new CachedDescriptorFile(stamp, List.copyOf(descriptors));
    }

    private UiSkinDescriptor descriptor(JsonObject object) {
        String id = string(object, "id", "");
        String source = firstString(object, "source", "image", "path", "texture");
        UiSkinType type = UiSkinType.parse(string(object, "type", "image"), UiSkinType.IMAGE);
        int frame = integer(object, "frame", integer(object, "index", 0));
        UiSkinRect sourceRect = rect(object.get("sourceRect"));
        UiSkinThreeSlice threeSlice = threeSlice(object);
        return new UiSkinDescriptor(id, type, source, frame, sourceRect, threeSlice);
    }

    private UiSkinThreeSlice threeSlice(JsonObject object) {
        JsonObject parts = object.has("parts") && object.get("parts").isJsonObject()
                ? object.getAsJsonObject("parts")
                : object;
        UiSkinRect left = rect(parts.get("left"));
        UiSkinRect middle = rect(parts.has("middle") ? parts.get("middle") : parts.get("center"));
        UiSkinRect right = rect(parts.get("right"));
        UiSliceScaleMode mode = UiSliceScaleMode.parse(firstNestedString(object, "middle", "mode", string(object, "mode", "stretch")), UiSliceScaleMode.STRETCH);
        return new UiSkinThreeSlice(left, middle, right, mode);
    }

    private UiSkinRect rect(JsonElement element) {
        if (element == null || element.isJsonNull()) return UiSkinRect.EMPTY;
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return new UiSkinRect(intAt(array, 0), intAt(array, 1), intAt(array, 2), intAt(array, 3));
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            return new UiSkinRect(integer(object, "x", 0), integer(object, "y", 0), integer(object, "w", integer(object, "width", 0)), integer(object, "h", integer(object, "height", 0)));
        }
        return UiSkinRect.EMPTY;
    }

    private JsonArray firstArray(JsonObject object, String... keys) {
        if (object == null || keys == null) return null;
        for (String key : keys) {
            JsonElement value = object.get(key);
            if (value != null && value.isJsonArray()) return value.getAsJsonArray();
        }
        return null;
    }

    private String firstString(JsonObject object, String... keys) {
        for (String key : keys) {
            String value = string(object, key, "");
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private String firstNestedString(JsonObject object, String objectKey, String fieldKey, String fallback) {
        JsonElement value = object.get(objectKey);
        if (value != null && value.isJsonObject()) return string(value.getAsJsonObject(), fieldKey, fallback);
        return fallback;
    }

    private String string(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString().trim();
    }

    private int integer(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return fallback;
        try {
            return Math.round(value.getAsFloat());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private int intAt(JsonArray array, int index) {
        if (array == null || index < 0 || index >= array.size()) return 0;
        try {
            return Math.round(array.get(index).getAsFloat());
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static FileStamp fileStamp(String path) {
        try {
            FileHandle file = DataFiles.file(path);
            if (file != null && file.exists()) {
                return new FileStamp(file.lastModified(), file.length(), true);
            }
        } catch (RuntimeException ignored) {
            // Virtual resources may not expose stable modification metadata.
        }
        return new FileStamp(-1L, -1L, false);
    }

    private static String abbreviate(Set<String> values) {
        int count = 0;
        StringBuilder out = new StringBuilder("[");
        for (String value : values) {
            if (count > 0) out.append(", ");
            if (count >= 8) {
                out.append("...");
                break;
            }
            out.append(value);
            count++;
        }
        return out.append(']').toString();
    }

    private record CachedDescriptorFile(FileStamp stamp, List<UiSkinDescriptor> descriptors) {
    }

    private record FileStamp(long modified, long length, boolean cacheable) {
    }
}
