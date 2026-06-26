package org.takesome.frozenlands.engine.world.sky;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeInstaller;

public final class SkyRuntimeInstaller implements EngineRuntimeInstaller {
    @Override
    public int priority() {
        return 200;
    }

    @Override
    public String id() {
        return SkyRuntimeModule.ID;
    }

    @Override
    public void install(EngineContext context) {
        Sky sky = new Sky(context);
        sky.addSky();
        context.registerService(Sky.class, sky);
        context.getModuleRegistry().register(new SkyRuntimeModule(sky), context);
    }
}
