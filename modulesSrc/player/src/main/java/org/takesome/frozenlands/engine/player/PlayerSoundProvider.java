package org.takesome.frozenlands.engine.player;

import com.jme3.audio.AudioNode;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.providers.sound.SoundProvider;

import java.util.List;
import java.util.Map;

public class PlayerSoundProvider extends SoundProvider {

    private Map<String, List<AudioNode>> playerSounds;

    public PlayerSoundProvider(EngineContext kernelInterface) {
        super(kernelInterface);
        playerSounds = kernelInterface.requireService(SoundProvider.class).getSoundBlock("player");
    }

    public void playSound(String sound){
        List<AudioNode> eventSounds = playerSounds.get(sound);
        if(eventSounds != null) {
            this.getRandomAudioNode(eventSounds).play();
        }
    }
}
