package org.takesome.frozenlands.engine.shaders;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeInstaller;

public final class ShadersRuntimeInstaller implements EngineRuntimeInstaller {
    @Override
    public int priority() {
        return 400;
    }

    @Override
    public String id() {
        return ShaderModule.ID;
    }

    @Override
    public void install(EngineContext context) {
        Shaders shaders = new Shaders(context);
        context.appStateManager().attach(shaders);
        context.registerService(Shaders.class, shaders);
        context.getModuleRegistry().register(new ShaderModule(shaders), context);
    }
}
