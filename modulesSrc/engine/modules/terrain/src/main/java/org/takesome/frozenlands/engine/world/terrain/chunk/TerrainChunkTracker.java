package org.takesome.frozenlands.engine.world.terrain.chunk;

import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TerrainChunkTracker {
    private final Map<String, TerrainChunkSnapshot> chunksByKey = new LinkedHashMap<>();
    private int attachedCount;
    private int detachedCount;

    public synchronized void tileAttached(Vector3f cell, TerrainQuad quad) {
        attachedCount++;
        String key = key(cell);
        chunksByKey.put(key, new TerrainChunkSnapshot(key, cell, quad.getName(), true, Instant.now()));
    }

    public synchronized void tileDetached(Vector3f cell, TerrainQuad quad) {
        detachedCount++;
        String key = key(cell);
        chunksByKey.put(key, new TerrainChunkSnapshot(key, cell, quad.getName(), false, Instant.now()));
    }

    public synchronized int getLoadedChunkCount() {
        int count = 0;
        for (TerrainChunkSnapshot snapshot : chunksByKey.values()) {
            if (snapshot.isAttached()) {
                count++;
            }
        }
        return count;
    }

    public synchronized List<Map<String, Object>> snapshotMaps() {
        return chunksByKey.values().stream().map(TerrainChunkSnapshot::toMap).toList();
    }

    public synchronized Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("knownChunks", chunksByKey.size());
        status.put("loadedChunks", getLoadedChunkCount());
        status.put("attachedEvents", attachedCount);
        status.put("detachedEvents", detachedCount);
        return status;
    }

    private String key(Vector3f cell) {
        return Math.round(cell.x) + ":" + Math.round(cell.z);
    }
}
