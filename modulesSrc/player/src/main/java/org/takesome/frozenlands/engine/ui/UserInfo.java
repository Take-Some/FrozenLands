package org.takesome.frozenlands.engine.ui;

import org.takesome.frozenlands.engine.player.Player;

@Deprecated
public final class UserInfo {
    private final PlayerHud hud;

    public UserInfo(Player player) {
        this.hud = new PlayerHud(player);
    }

    public PlayerHud hud() {
        return hud;
    }
}
