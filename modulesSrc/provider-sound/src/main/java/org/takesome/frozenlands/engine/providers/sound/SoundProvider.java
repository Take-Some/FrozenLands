package org.takesome.frozenlands.engine.providers.sound;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventPayload;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.events.EventSubscriptionBag;
import org.takesome.frozenlands.engine.providers.EngineProvider;
import org.takesome.frozenlands.engine.providers.EngineProviders;
import org.takesome.frozenlands.engine.providers.ProviderCommand;
import org.takesome.frozenlands.engine.resources.ModuleIndexCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SoundProvider implements EngineProvider {
    private static final ModuleIndexCatalog MODULE_INDEX = ModuleIndexCatalog.defaultCatalog();
    private static final String DEFAULT_SOUNDS_CONFIG = MODULE_INDEX.configPath(EngineProviders.SOUND_PROVIDER, "sounds");

    private final EngineContext engineContext;
    private final SoundRegistry registry = new SoundRegistry();
    private final Map<String, ProviderCommand> commands = new LinkedHashMap<>();
    private final EventSubscriptionBag subscriptions = new EventSubscriptionBag();
    private String loadedPath = "";

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
        subscribeToEvents();
    }

    @Override
    public void unregister(EngineContext context) {
        closeSubscriptions();
    }

    @Override
    public Map<String, ProviderCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    @Override
    public Map<String, Object> luaDescriptor() {
        Map<String, Object> descriptor = EngineProvider.super.luaDescriptor();
        descriptor.put("blocks", registry.blocks());
        descriptor.put("status", registry.status());
        descriptor.put("eventDriven", true);
        return descriptor;
    }

    public SoundRegistry registry() {
        return registry;
    }

    public void loadSounds(String path) {
        registry.clear();
        loadedPath = path;
        JsonNode jsonRoot = MODULE_INDEX.readJsonNode(path);
        jsonRoot.fieldNames().forEachRemaining(block -> loadBlock(block, jsonRoot.get(block)));
        Map<String, Object> event = registry.status();
        event.put("path", path);
        engineContext.getProviderRegistry().publishEvent("provider.sound.loaded", event);
        engineContext.getModuleRegistry().publishEvent("provider.sound.loaded", event);
        engineContext.getLogger().info("SoundRegistry loaded path={} blocks={} events={} variants={}",
                path,
                event.get("blocks"),
                event.get("events"),
                event.get("variants")
        );
    }

    public Map<String, List<AudioNode>> getSoundBlock(String blockName) {
        return registry.legacyBlock(blockName);
    }

    public boolean play(String blockName, String eventName) {
        return play(blockName, eventName, "direct", Map.of()).played();
    }

    public SoundRegistry.PlayResult play(String blockName, String eventName, String source, Map<String, Object> requestPayload) {
        SoundRegistry.PlayResult result = registry.play(blockName, eventName);
        Map<String, Object> payload = new LinkedHashMap<>(result.toMap());
        payload.put("source", source == null ? "unknown" : source);
        if (requestPayload != null && !requestPayload.isEmpty()) {
            payload.put("request", EngineEventPayload.copy(requestPayload));
        }

        String providerTopic = result.played() ? "provider.sound.play" : "provider.sound.play.failed";
        String moduleTopic = result.played() ? EngineEventTopics.ENGINE_SOUND_PLAYED : EngineEventTopics.ENGINE_SOUND_PLAY_FAILED;
        engineContext.getProviderRegistry().publishEvent(providerTopic, payload);
        engineContext.getModuleRegistry().publishEvent(moduleTopic, payload);
        if (!result.played()) {
            engineContext.getLogger().warn("Sound play failed block={} event={} reason={} source={}", blockName, eventName, result.reason(), source);
        }
        return result;
    }

    public Map<String, Object> requestPlay(String block, String event, String source, Map<String, Object> extraPayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("block", block == null || block.isBlank() ? "player" : block);
        payload.put("event", event);
        payload.put("source", source == null || source.isBlank() ? "request" : source);
        if (extraPayload != null) {
            payload.putAll(extraPayload);
        }
        engineContext.getModuleRegistry().publishEvent(EngineEventTopics.ENGINE_SOUND_PLAY_REQUESTED, payload);
        return result("requested", true);
    }

    public void update(float tpf) {
        registry.update(tpf);
    }

    public AudioNode getRandomAudioNode(List<AudioNode> sndList) {
        return registry.randomAudioNode(sndList);
    }

    @Deprecated
    public Map<String, Map<String, List<AudioNode>>> getSounds() {
        Map<String, Map<String, List<AudioNode>>> result = new LinkedHashMap<>();
        for (String block : registry.blocks()) {
            Map<String, List<AudioNode>> soundBlock = registry.legacyBlock(block);
            if (soundBlock != null) {
                result.put(block, soundBlock);
            }
        }
        return result;
    }

    private void subscribeToEvents() {
        if (!subscriptions.isEmpty()) {
            return;
        }
        subscriptions.add(engineContext.getModuleRegistry().getEventBus().subscribe(
                EngineEventTopics.ENGINE_SOUND_PLAY_REQUESTED,
                this::handleSoundRequest));
        engineContext.getLogger().info("SoundProvider event subscriptions installed topics={}", subscriptions.size());
    }

    @SuppressWarnings("unchecked")
    private void handleSoundRequest(Map<String, Object> event) {
        Map<String, Object> payload = EngineEventPayload.of(event);
        String block = EngineEventPayload.string(payload, "block", "player");
        String soundEvent = EngineEventPayload.string(payload, "event", EngineEventPayload.string(payload, "sound", ""));
        String source = EngineEventPayload.string(payload, "source", EngineEventTopics.ENGINE_SOUND_PLAY_REQUESTED);
        if (soundEvent == null || soundEvent.isBlank()) {
            play(block, soundEvent, source, payload);
            return;
        }
        play(block, soundEvent, source, payload);
    }

    private void closeSubscriptions() {
        subscriptions.close();
    }

    private void loadBlock(String block, JsonNode eventsArray) {
        if (eventsArray == null || !eventsArray.isArray()) {
            engineContext.getLogger().warn("Sound block ignored because it is not an array: {}", block);
            return;
        }
        engineContext.getLogger().debug("SoundRegistry scanning block {}", block);
        eventsArray.forEach(eventNode -> loadSoundEvent(block, eventNode));
    }

    private void loadSoundEvent(String block, JsonNode eventNode) {
        String event = requiredText(eventNode, "event", "<missing-event>");
        String packagePath = text(eventNode, "package", "");
        JsonNode settingsNode = eventNode.get("settings");
        AudioData.DataType dataType = AudioData.DataType.valueOf(text(settingsNode, "dataType", "Buffer"));
        float cooldown = floating(settingsNode, "cooldown", 0f);
        List<SoundRegistry.SoundVariant> variants = new ArrayList<>();
        JsonNode soundsArray = eventNode.get("sounds");
        if (soundsArray != null && soundsArray.isArray()) {
            soundsArray.forEach(soundNode -> {
                String declaredPath = soundNode.asText();
                String resolvedPath = resolveSoundPath(block, packagePath, event, declaredPath);
                AudioNode audioNode = new AudioNode(engineContext.getAssetManager(), resolvedPath, dataType);
                applySettings(audioNode, settingsNode);
                variants.add(new SoundRegistry.SoundVariant(resolvedPath, audioNode));
            });
        }
        registry.register(new SoundRegistry.SoundEvent(block, event, packagePath, variants, cooldown));
        engineContext.getLogger().debug("SoundRegistry registered block={} event={} variants={}", block, event, variants.size());
    }

    private String resolveSoundPath(String block, String packagePath, String event, String declaredPath) {
        if (declaredPath == null || declaredPath.isBlank()) {
            return "";
        }
        String normalized = declaredPath.replace('\\', '/');
        if (normalized.startsWith("sounds/")) {
            return normalized;
        }
        if (normalized.startsWith("/sounds/")) {
            return normalized.substring(1);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return "sounds/" + block + '/' + normalizePackage(packagePath) + event + normalized;
    }

    private String normalizePackage(String packagePath) {
        if (packagePath == null || packagePath.isBlank()) {
            return "";
        }
        String normalized = packagePath.replace('\\', '/');
        return normalized.endsWith("/") ? normalized : normalized + '/';
    }

    private void applySettings(AudioNode audioNode, JsonNode settingsNode) {
        if (settingsNode == null) {
            return;
        }
        audioNode.setVolume(floating(settingsNode, "volume", audioNode.getVolume()));
        audioNode.setPositional(bool(settingsNode, "positional", audioNode.isPositional()));
        audioNode.setPitch(floating(settingsNode, "pitch", audioNode.getPitch()));
        if (settingsNode.has("looping")) {
            audioNode.setLooping(settingsNode.get("looping").asBoolean(false));
        }
        if (settingsNode.has("reverb")) {
            audioNode.setReverbEnabled(settingsNode.get("reverb").asBoolean(false));
        }
    }

    private void registerCommands() {
        commands.put("status", ProviderCommand.of("status", "Return sound registry status", args -> status()));
        commands.put("load", ProviderCommand.of("load", "Load sound declarations from a resource path", args -> {
            String path = stringArg(args, "path", DEFAULT_SOUNDS_CONFIG);
            loadSounds(path);
            return status();
        }));
        commands.put("reload", ProviderCommand.of("reload", "Reload current sound declarations", args -> {
            loadSounds(loadedPath == null || loadedPath.isBlank() ? DEFAULT_SOUNDS_CONFIG : loadedPath);
            return status();
        }));
        commands.put("list.blocks", ProviderCommand.of("list.blocks", "List sound blocks", args -> result("blocks", registry.blocks())));
        commands.put("list.events", ProviderCommand.of("list.events", "List events for one sound block", args -> result("events", registry.events(stringArg(args, "block", "")))));
        commands.put("event.get", ProviderCommand.of("event.get", "Return one sound event descriptor", args -> registry.describeEvent(stringArg(args, "block", ""), stringArg(args, "event", ""))));
        commands.put("registry.snapshot", ProviderCommand.of("registry.snapshot", "Return full sound registry snapshot", args -> result("events", registry.snapshot())));
        commands.put("request", ProviderCommand.of("request", "Publish an event-driven sound request", args ->
                requestPlay(stringArg(args, "block", "player"), stringArg(args, "event", stringArg(args, "sound", "")), "engine.sound.command", args)));
        commands.put("play", ProviderCommand.of("play", "Play one sound event immediately", args -> {
            String block = stringArg(args, "block", "player");
            String event = stringArg(args, "event", "");
            SoundRegistry.PlayResult playResult = play(block, event, "engine.sound.command.play", args);
            Map<String, Object> result = result("played", playResult.played());
            result.putAll(playResult.toMap());
            return result;
        }));
    }

    private Map<String, Object> status() {
        Map<String, Object> result = registry.status();
        result.put("path", loadedPath);
        result.put("eventDriven", true);
        result.put("subscriptions", subscriptions.size());
        return result;
    }

    private String requiredText(JsonNode node, String name, String fallback) {
        return text(node, name, fallback);
    }

    private String text(JsonNode node, String name, String fallback) {
        if (node == null || !node.has(name) || node.get(name).isNull()) {
            return fallback;
        }
        return node.get(name).asText(fallback);
    }

    private float floating(JsonNode node, String name, float fallback) {
        if (node == null || !node.has(name) || node.get(name).isNull()) {
            return fallback;
        }
        return (float) node.get(name).asDouble(fallback);
    }

    private boolean bool(JsonNode node, String name, boolean fallback) {
        if (node == null || !node.has(name) || node.get(name).isNull()) {
            return fallback;
        }
        return node.get(name).asBoolean(fallback);
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
