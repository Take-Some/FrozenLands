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
        PlayerManager playerManager = new PlayerManager(context);
        PlayerFeedbackRouter feedbackRouter = new PlayerFeedbackRouter(context);
        context.registerService(PlayerManager.class, playerManager);
        context.registerService(PlayerFeedbackRouter.class, feedbackRouter);
        feedbackRouter.start();
        context.appStateManager().attach(playerManager);
        context.appStateManager().attach(new PlayerLuaEventHookState(context));
        context.getModuleRegistry().register(new PlayerModule(context), context);
    }
}
