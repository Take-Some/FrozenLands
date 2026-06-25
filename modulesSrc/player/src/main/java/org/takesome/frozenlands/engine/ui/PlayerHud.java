package org.takesome.frozenlands.engine.ui;

import org.takesome.frozenlands.engine.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerHud {
    private final Player player;
    private final Map<String, String> labels = new LinkedHashMap<>();
    private boolean initialized;

    public PlayerHud(Player player) {
        this.player = player;
    }

    public void initialize() {
        initialized = true;
        labels.put("posX", "0");
        labels.put("posY", "0");
        labels.put("posZ", "0");
    }

    public void updateLabelTexts(String[] keys, String[] values) {
        if (!initialized) {
            initialize();
        }
        for (int index = 0; index < keys.length && index < values.length; index++) {
            labels.put(keys[index], values[index]);
        }
    }

    public Map<String, String> snapshot() {
        return Map.copyOf(labels);
    }
}
