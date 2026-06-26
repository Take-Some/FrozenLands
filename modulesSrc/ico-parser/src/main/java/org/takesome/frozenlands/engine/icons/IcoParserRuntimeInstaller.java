package org.takesome.frozenlands.engine.icons;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeInstaller;

public final class IcoParserRuntimeInstaller implements EngineRuntimeInstaller {
    @Override
    public int priority() {
        return 900;
    }

    @Override
    public String id() {
        return IcoParserModule.MODULE_ID;
    }

    @Override
    public void install(EngineContext context) {
        context.getModuleRegistry().register(new IcoParserModule(), context);
    }
}
