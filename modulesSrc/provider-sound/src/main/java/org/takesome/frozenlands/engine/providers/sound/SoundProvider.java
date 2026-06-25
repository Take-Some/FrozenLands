package org.takesome.frozenlands.engine.providers.sound;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.providers.EngineProvider;
import org.takesome.frozenlands.engine.providers.EngineProviders;
import org.takesome.frozenlands.engine.providers.ProviderCommand;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


public class SoundProvider implements EngineProvider {
    private static final ModuleIndexCatalog MODULE_INDEX = ModuleIndexCatalog.defaultCatalog();
    private static final String DEFAULT_SOUNDS_CONFIG = MODULE_INDEX.configPath(EngineProviders.SOUND_PROVIDER, "sounds");

    private final EngineContext engineContext;
    private final Map<String, Map<String, List<AudioNode>>> sounds = new LinkedHashMap<>();
    private final Map<String, ProviderCommand> commands = new LinkedHashMap<>();
    private final Random random = new Random();
    private int totalSounds = 0;

    public SoundProvider(EngineContext engineContext) {
        this.engineContext = engineContext;
        registerCommands();
    }

    @Override
    public String id() {
        return EngineProviders.SOUND_PROVIDER;
    }

    @Override
    public void register(EngineContext context) {
        loadSounds(DEFAULT_SOUNDS_CONFIG);
    }

    @Override
    public Map<String, ProviderCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    @Override
    public Map<String, Object> luaDescriptor() {
        Map<String, Object> descriptor = EngineProvider.super.luaDescriptor();
        descriptor.put("blocks", List.copyOf(sounds.keySet()));
        descriptor.put("totalSounds", totalSounds);
        return descriptor;
    }

    public void loadSounds(String path) {
        sounds.clear();
        totalSounds = 0;

        JsonNode jsonRoot = MODULE_INDEX.readJsonNode(path);
        Iterator<String> iterator = jsonRoot.fieldNames();
        while (iterator.hasNext()) {
            String currentBlock = iterator.next();
            engineContext.getLogger().info("====== Scanning block " + currentBlock + " ======");
            JsonNode eventsArray = jsonRoot.get(currentBlock);
            Map<String, List<AudioNode>> soundBlock = new LinkedHashMap<>();
            eventsArray.forEach(eventNode -> loadSoundEvent(currentBlock, soundBlock, eventNode));
            sounds.put(currentBlock, soundBlock);
        }

        Map<String, Object> event = result("blocks", sounds.size());
        event.put("totalSounds", totalSounds);
        event.put("path", path);
        engineContext.getProviderRegistry().publishEvent("provider.sound.loaded", event);
        engineContext.getLogger().info("Finished adding sounds, total sndAmount: " + sounds.size() + "x" + totalSounds);
    }

    public Map<String, List<AudioNode>> getSoundBlock(String blockName) {
        return sounds.get(blockName);
    }

    public boolean play(String blockName, String eventName) {
        Map<String, List<AudioNode>> soundBlock = sounds.get(blockName);
        if (soundBlock == null) {
            return false;
        }
        AudioNode node = getRandomAudioNode(soundBlock.get(eventName));
        if (node == null) {
            return false;
        }
        node.play();
        engineContext.getProviderRegistry().publishEvent("provider.sound.play", Map.of("block", blockName, "event", eventName));
        return true;
    }

    public void update(float tpf) {
        for (Map.Entry<String, Map<String, List<AudioNode>>> entry : sounds.entrySet()) {
            for (Map.Entry<String, List<AudioNode>> update : entry.getValue().entrySet()) {
                Iterator<AudioNode> iterator = update.getValue().iterator();
                while (iterator.hasNext()) {
                    updateSoundNode(tpf, iterator, iterator.next());
                }
            }
        }
    }

    public AudioNode getRandomAudioNode(List<AudioNode> sndList) {
        if (sndList != null && !sndList.isEmpty()) {
            return sndList.get(random.nextInt(sndList.size()));
        }
        return null;
    }

    @Deprecated
    public Map<String, Map<String, List<AudioNode>>> getSounds() {
        return sounds;
    }

    private void loadSoundEvent(String currentBlock, Map<String, List<AudioNode>> soundBlock, JsonNode eventNode) {
        AtomicInteger soundsNum = new AtomicInteger();
        String event = eventNode.get("event").asText();
        String sndPackage = eventNode.has("package") ? eventNode.get("package").asText() : "";
        JsonNode settingsNode = eventNode.get("settings");
        List<AudioNode> audioNodes = new ArrayList<>();
        JsonNode soundsArray = eventNode.get("sounds");
        soundsArray.forEach(soundNode -> {
            String fileName = soundNode.asText();
            AudioData.DataType dataType = AudioData.DataType.valueOf(settingsNode.get("dataType").asText());
            String filePath = "sounds/" + currentBlock + '/' + sndPackage + event + fileName;
            AudioNode audioNode = new AudioNode(engineContext.getAssetManager(), filePath, dataType);
            applySettings(audioNode, settingsNode);
            audioNodes.add(audioNode);
            soundsNum.incrementAndGet();
            totalSounds++;
        });
        engineContext.getLogger().info("Added " + soundsNum + " sounds to '" + event + "' event");
        soundBlock.put(event, audioNodes);
    }

    private void applySettings(AudioNode audioNode, JsonNode settingsNode) {
        if (settingsNode == null) {
            return;
        }
        if (settingsNode.has("volume")) {
            audioNode.setVolume((float) settingsNode.get("volume").asDouble());
        }
        if (settingsNode.has("positional")) {
            audioNode.setPositional(settingsNode.get("positional").asBoolean(false));
        }
        if (settingsNode.has("pitch")) {
            audioNode.setPitch((float) settingsNode.get("pitch").asDouble());
        }
    }

    private void updateSoundNode(float tpf, Iterator<AudioNode> iterator, AudioNode audioNode) {
        boolean removeAfterPlayback = audioNode.getUserData("removeAfterPlayback") != null
                ? audioNode.getUserData("removeAfterPlayback") : false;
        if (removeAfterPlayback && audioNode.getStatus() == AudioSource.Status.Stopped) {
            audioNode.removeFromParent();
            iterator.remove();
        }

        float cooldown = audioNode.getUserData("cooldown") != null ? audioNode.getUserData("cooldown") : -1f;
        if (cooldown > 0) {
            cooldown -= tpf;
            audioNode.setUserData("cooldown", cooldown <= 0 ? null : cooldown);
        }
    }

    private void registerCommands() {
        commands.put("load", ProviderCommand.of("load", "Load sound declarations from a resource path", args -> {
            String path = stringArg(args, "path", DEFAULT_SOUNDS_CONFIG);
            loadSounds(path);
            Map<String, Object> result = result("blocks", sounds.size());
            result.put("totalSounds", totalSounds);
            return result;
        }));
        commands.put("list.blocks", ProviderCommand.of("list.blocks", "List sound blocks", args -> result("blocks", List.copyOf(sounds.keySet()))));
        commands.put("list.events", ProviderCommand.of("list.events", "List events for one sound block", args -> {
            String block = stringArg(args, "block", "");
            Map<String, List<AudioNode>> soundBlock = sounds.getOrDefault(block, new HashMap<>());
            return result("events", List.copyOf(soundBlock.keySet()));
        }));
        commands.put("play", ProviderCommand.of("play", "Play one sound event", args -> {
            String block = stringArg(args, "block", "player");
            String event = stringArg(args, "event", "");
            Map<String, Object> result = result("played", play(block, event));
            result.put("block", block);
            result.put("event", event);
            return result;
        }));
    }

    private String stringArg(Map<String, Object> args, String name, String fallback) {
        Object value = args == null ? null : args.get(name);
        return value == null ? fallback : String.valueOf(value);
    }

    private Map<String, Object> result(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put(key, value);
        return result;
    }
}
