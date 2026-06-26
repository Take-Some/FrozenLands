package org.takesome.frozenlands.engine.world;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.world.effect.ParticleManager;

public class WorldUpdate extends BaseAppState {
    private final ParticleManager particleManager;

    public WorldUpdate(EngineContext engineContext){
        this.particleManager = new ParticleManager(engineContext);
    }

    @Override
    protected void initialize(Application application) {
        particleManager.initialize();
    }

    @Override
    protected void cleanup(Application application) {
        particleManager.cleanup();
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update(float tpf){
        particleManager.update(tpf);
    }

    public ParticleManager getParticleManager() {
        return particleManager;
    }
}
