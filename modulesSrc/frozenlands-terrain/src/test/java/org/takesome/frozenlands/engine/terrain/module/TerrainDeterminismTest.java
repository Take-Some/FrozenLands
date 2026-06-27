package org.takesome.frozenlands.engine.terrain.module;

import org.junit.jupiter.api.Test;
import org.takesome.frozenlands.engine.terrain.TerrainService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

final class TerrainDeterminismTest {
    @Test
    void sameSeedAndPositionReturnSameSample() {
        TerrainService service = TerrainServiceFactory.create(null);
        TerrainService.TerrainSample first = service.sample(42L, 12.25f, -4.75f);
        TerrainService.TerrainSample second = service.sample(42L, 12.25f, -4.75f);

        assertEquals(first.baseHeight(), second.baseHeight(), 0f);
        assertEquals(first.walkableHeight(), second.walkableHeight(), 0f);
        assertEquals(first.biome(), second.biome());
        assertEquals(first.surface(), second.surface());
    }

    @Test
    void chunkCacheIsDeterministicAndInvalidatable() {
        TerrainService service = TerrainServiceFactory.create(null);
        TerrainService.TerrainChunk first = service.chunk(7L, 1, -2);
        TerrainService.TerrainChunk second = service.chunk(7L, 1, -2);

        assertSame(first, second);
        service.invalidateChunk(7L, 1, -2);
        TerrainService.TerrainChunk third = service.chunk(7L, 1, -2);
        assertNotSame(first, third);
    }
}
