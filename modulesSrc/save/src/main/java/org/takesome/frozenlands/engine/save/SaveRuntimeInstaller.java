package org.takesome.frozenlands.engine.save;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeInstaller;

public final class SaveRuntimeInstaller implements EngineRuntimeInstaller {
    @Override
    public int priority() {
        return 800;
    }

    @Override
    public String id() {
        return SaveModule.ID;
    }

    @Override
    public void install(EngineContext context) {
        SaveManager saveManager = new SaveManager(context);
        context.registerService(SaveManager.class, saveManager);
        context.getModuleRegistry().register(new SaveModule(saveManager), context);
    }
}
