package org.takesome.frozenlands.engine.player;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventTopics;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerSoundProvider {
    private final EngineContext engineContext;

    public PlayerSoundProvider(EngineContext kernelInterface) {
        this.engineContext = kernelInterface;
    }

    public boolean playSound(String sound) {
        return requestSound(sound, "player.sound.provider");
    }

    public boolean requestSound(String sound, String source) {
        if (sound == null || sound.isBlank()) {
            return false;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("block", "player");
        payload.put("event", sound);
        payload.put("source", source == null || source.isBlank() ? "player" : source);
        engineContext.getModuleRegistry().publishEvent(EngineEventTopics.ENGINE_SOUND_PLAY_REQUESTED, payload);
        return true;
    }
}
