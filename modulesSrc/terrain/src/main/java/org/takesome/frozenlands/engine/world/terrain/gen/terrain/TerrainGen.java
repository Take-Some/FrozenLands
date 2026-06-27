package org.takesome.frozenlands.engine.world.terrain.gen.terrain;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.UpdateControl;
import com.jme3.terrain.geomipmap.TerrainGrid;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.grid.FractalTileLoader;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.providers.material.MaterialProvider;
import org.takesome.frozenlands.engine.world.terrain.TerrainRuntimeSettings;
import org.takesome.frozenlands.engine.world.terrain.chunk.TerrainChunkTracker;

public class TerrainGen {
    private final EngineContext kernelInterface;
    private TerrainGenHelper terrainGenHelper;
    private FractalSum base;
    private PerturbFilter perturb;
    private OptimizedErode therm;
    private SmoothFilter smooth;
    private IterativeFilter iterate;
    private final TerrainChunkTracker chunkTracker;
    private final TerrainRuntimeSettings settings;

    public TerrainGen(EngineContext kernelInterface, TerrainChunkTracker chunkTracker) {
        this.kernelInterface = kernelInterface;
        this.chunkTracker = chunkTracker;
        this.settings = new TerrainRuntimeSettings();
    }

    public TerrainQuad generateTerrain(float baseRoughness, float baseFrequency, float baseAmplitude, float baseLacunarity, int baseOctaves, float baseScale) {
        Material matTerrain = this.kernelInterface.requireService(MaterialProvider.class).getMaterial(settings.terrainMaterial());

        this.base = new FractalSum();
        this.base.setRoughness(baseRoughness);
        this.base.setFrequency(baseFrequency);
        this.base.setAmplitude(baseAmplitude);
        this.base.setLacunarity(baseLacunarity);
        this.base.setOctaves(baseOctaves);
        this.base.setScale(baseScale);
        this.base.addModulator(
                (NoiseModulator) in -> ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1));

        FilteredBasis ground = new FilteredBasis(this.base);

        this.perturb = new PerturbFilter();
        this.perturb.setMagnitude(settings.perturbMagnitude());

        this.therm = new OptimizedErode();
        this.therm.setRadius(settings.erosionRadius());
        this.therm.setTalus(settings.erosionTalus());

        this.smooth = new SmoothFilter();
        this.smooth.setRadius(settings.terrainSmoothRadius());
        this.smooth.setEffect(settings.terrainSmoothEffect());

        this.iterate = new IterativeFilter();
        this.iterate.addPreFilter(this.perturb);
        this.iterate.addPostFilter(this.smooth);
        this.iterate.setFilter(this.therm);
        this.iterate.setIterations(settings.filterIterations());

        ground.addPreFilter(this.iterate);

        TerrainQuad terrain = new TerrainGrid(
                "terrain",
                settings.patchSize(),
                settings.quadSize(),
                new FractalTileLoader(ground, settings.tileNoiseScale())
        ) {
            private boolean isNeighbour(int quadIndex) {
                return quadIndex == 0 || quadIndex == 1 || quadIndex == 2
                        || quadIndex == 3 || quadIndex == 4 || quadIndex == 8
                        || quadIndex == 7 || quadIndex == 11 || quadIndex == 12
                        || quadIndex == 13 || quadIndex == 14 || quadIndex == 15;
            }

            class UpdateQuadCacheRpg extends UpdateQuadCache {
                public UpdateQuadCacheRpg(Vector3f location) {
                    super(location);
                }

                @Override
                public void run() {
                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 4; j++) {
                            int quadIdx = i * 4 + j;
                            final Vector3f quadCell = location.add(quadIndex[quadIdx]);
                            TerrainQuad q = cache.get(quadCell);
                            if (q == null && getGridTileLoader() != null) {
                                q = getGridTileLoader().getTerrainQuadAt(quadCell);
                                if (q.getMaterial() == null) {
                                    q.setMaterial(material.clone());
                                }
                                kernelInterface.getLogger().debug("Loaded TerrainQuad {} from TerrainQuadGrid", q.getName());
                            }
                            cache.put(quadCell, q);

                            final int quadrant = getQuadrant(quadIdx);
                            final TerrainQuad newQuad = q;

                            if (!isNeighbour(quadIdx)) {
                                if (isCenter(quadIdx)) {
                                    getControl(UpdateControl.class).enqueue(() -> {
                                        attachQuadAt(newQuad, quadrant, quadCell, newQuad.getParent() != null);
                                        return null;
                                    });
                                } else {
                                    getControl(UpdateControl.class).enqueue(() -> {
                                        removeQuad(newQuad);
                                        kernelInterface.getLogger().info("Unloaded TerrainQuad " + newQuad.getQuadrant() + " from TerrainQuadGrid");
                                        return null;
                                    });
                                }
                            }
                        }
                    }

                    getControl(UpdateControl.class).enqueue(() -> {
                        for (Spatial s : getChildren()) {
                            if (s instanceof TerrainQuad tq) {
                                tq.resetCachedNeighbours();
                            }
                        }
                        setNeedToRecalculateNormals();
                        return null;
                    });
                }
            }

            @Override
            protected void updateChildren(Vector3f camCell) {
                int dx = 0;
                int dy = 0;
                if (currentCamCell != null) {
                    dx = (int) (camCell.x - currentCamCell.x);
                    dy = (int) (camCell.z - currentCamCell.z);
                }

                int xMin = 0;
                int xMax = 4;
                int yMin = 0;
                int yMax = 4;
                if (dx == -1) {
                    xMax = 3;
                } else if (dx == 1) {
                    xMin = 1;
                }

                if (dy == -1) {
                    yMax = 3;
                } else if (dy == 1) {
                    yMin = 1;
                }

                for (int i = yMin; i < yMax; i++) {
                    for (int j = xMin; j < xMax; j++) {
                        cache.get(camCell.add(quadIndex[i * 4 + j]));
                    }
                }

                if (cacheExecutor == null) {
                    cacheExecutor = createExecutorService();
                }
                kernelInterface.getLogger().debug("TerrainGrid camera cell {}", camCell);
                cacheExecutor.submit(new UpdateQuadCacheRpg(camCell));

                this.currentCamCell = camCell;
            }
        };

        terrain.setMaterial(matTerrain);
        terrainGenHelper = new TerrainGenHelper(kernelInterface, terrain, chunkTracker);
        terrainGenHelper.setupPosition();
        terrainGenHelper.setupScale();
        terrainGenHelper.setUpLODControl();
        terrainGenHelper.setUpCollision();

        terrain.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        return terrain;
    }
}
