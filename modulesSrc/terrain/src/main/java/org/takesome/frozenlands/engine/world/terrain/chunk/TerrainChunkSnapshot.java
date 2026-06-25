package org.takesome.frozenlands.engine.world.terrain.chunk;

import com.jme3.math.Vector3f;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TerrainChunkSnapshot {
    private final String key;
    private final Vector3f cell;
    private final String quadName;
    private final boolean attached;
    private final Instant updatedAt;

    public TerrainChunkSnapshot(String key, Vector3f cell, String quadName, boolean attached, Instant updatedAt) {
        this.key = key;
        this.cell = cell.clone();
        this.quadName = quadName;
        this.attached = attached;
        this.updatedAt = updatedAt;
    }

    public String getKey() {
        return key;
    }

    public boolean isAttached() {
        return attached;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", key);
        map.put("x", cell.x);
        map.put("y", cell.y);
        map.put("z", cell.z);
        map.put("quadName", quadName);
        map.put("attached", attached);
        map.put("updatedAt", updatedAt.toString());
        return map;
    }
}
