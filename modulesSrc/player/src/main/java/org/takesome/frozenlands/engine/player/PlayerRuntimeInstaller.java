package org.takesome.frozenlands.engine.player;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeInstaller;

public final class PlayerRuntimeInstaller implements EngineRuntimeInstaller {
    @Override
    public int priority() {
        return 550;
    }

    @Override
    public String id() {
        return PlayerModule.ID;
    }

    @Override
    public void install(EngineContext context) {
        context.getModuleRegistry().register(new PlayerModule(context), context);
    }
}
