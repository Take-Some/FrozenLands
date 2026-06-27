package org.takesome.frozenlands.engine.world.effect;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeInstaller;
import org.takesome.frozenlands.engine.world.WorldUpdate;

public final class ParticlesRuntimeInstaller implements EngineRuntimeInstaller {
    @Override
    public int priority() {
        return 500;
    }

    @Override
    public String id() {
        return ParticleModule.ID;
    }

    @Override
    public void install(EngineContext context) {
        WorldUpdate worldUpdate = new WorldUpdate(context);
        context.appStateManager().attach(worldUpdate);
        context.registerService(WorldUpdate.class, worldUpdate);
        context.registerService(ParticleManager.class, worldUpdate.getParticleManager());
        context.registerService("particles.effects", ParticleEffectRegistry.class, worldUpdate.getParticleManager().effectRegistry());
        context.getModuleRegistry().register(new ParticleModule(worldUpdate.getParticleManager()), context);
    }
}
