package org.takesome.frozenlands.engine.world.terrain.chunk;

import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TerrainChunkTracker {
    private final Map<String, TerrainChunkSnapshot> chunksByKey = new LinkedHashMap<>();
    private final Set<String> collisionReadyByKey = new LinkedHashSet<>();
    private int attachedCount;
    private int detachedCount;
    private int collisionInstalledCount;
    private int collisionRemovedCount;

    public synchronized void tileAttached(Vector3f cell, TerrainQuad quad) {
        attachedCount++;
        String key = key(cell);
        chunksByKey.put(key, new TerrainChunkSnapshot(key, cell, quad.getName(), true, Instant.now()));
    }

    public synchronized void tileDetached(Vector3f cell, TerrainQuad quad) {
        detachedCount++;
        String key = key(cell);
        chunksByKey.put(key, new TerrainChunkSnapshot(key, cell, quad.getName(), false, Instant.now()));
        collisionRemovedCount++;
        collisionReadyByKey.clear();
    }

    public synchronized void tileCollisionInstalled(Vector3f cell, TerrainQuad quad) {
        collisionInstalledCount++;
        String key = key(cell);
        chunksByKey.put(key, new TerrainChunkSnapshot(key, cell, quad.getName(), true, Instant.now()));
        collisionReadyByKey.add(key);
    }

    public synchronized boolean hasCollisionReadyChunks() {
        return !collisionReadyByKey.isEmpty();
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
        status.put("collisionInstalledEvents", collisionInstalledCount);
        status.put("collisionRemovedEvents", collisionRemovedCount);
        status.put("collisionReady", hasCollisionReadyChunks());
        status.put("collisionReadyChunks", collisionReadyByKey.size());
        return status;
    }

    private String key(Vector3f cell) {
        return Math.round(cell.x) + ":" + Math.round(cell.z);
    }
}
