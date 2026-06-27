package org.takesome.frozenlands.engine.providers.sound;

import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public final class SoundRegistry {
    private final Map<String, SoundBlock> blocks = new LinkedHashMap<>();
    private final Random random = new Random();
    private int totalVariants;
    private long playRequests;
    private long playSuccesses;
    private long playMisses;

    public synchronized void clear() {
        blocks.clear();
        totalVariants = 0;
        playRequests = 0;
        playSuccesses = 0;
        playMisses = 0;
    }

    public synchronized SoundEvent register(SoundEvent event) {
        SoundBlock block = blocks.computeIfAbsent(event.block(), SoundBlock::new);
        SoundEvent previous = block.events.put(event.event(), event);
        if (previous != null) {
            totalVariants -= previous.size();
        }
        totalVariants += event.size();
        return event;
    }

    public synchronized Optional<SoundBlock> findBlock(String block) {
        return Optional.ofNullable(blocks.get(block));
    }

    public synchronized Optional<SoundEvent> findEvent(String block, String event) {
        SoundBlock soundBlock = blocks.get(block);
        if (soundBlock == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(soundBlock.events.get(event));
    }

    public synchronized PlayResult play(String block, String event) {
        playRequests++;
        SoundEvent soundEvent = findEvent(block, event).orElse(null);
        if (soundEvent == null) {
            playMisses++;
            return PlayResult.missed(block, event, "event-not-found");
        }
        AudioNode node = soundEvent.randomPlayable(random);
        if (node == null) {
            playMisses++;
            return PlayResult.missed(block, event, "no-playable-variant");
        }
        if (node.getStatus() == AudioSource.Status.Playing) {
            node.stop();
        }
        node.play();
        soundEvent.markPlayed(node);
        playSuccesses++;
        return PlayResult.played(block, event, soundEvent.indexOf(node), soundEvent.size(), soundEvent.pathOf(node));
    }

    public synchronized void update(float tpf) {
        for (SoundBlock block : blocks.values()) {
            for (SoundEvent event : block.events.values()) {
                event.update(tpf);
            }
        }
    }

    public synchronized List<String> blocks() {
        return List.copyOf(blocks.keySet());
    }

    public synchronized List<String> events(String block) {
        SoundBlock soundBlock = blocks.get(block);
        if (soundBlock == null) {
            return List.of();
        }
        return List.copyOf(soundBlock.events.keySet());
    }

    public synchronized Map<String, List<AudioNode>> legacyBlock(String block) {
        SoundBlock soundBlock = blocks.get(block);
        if (soundBlock == null) {
            return null;
        }
        Map<String, List<AudioNode>> legacy = new LinkedHashMap<>();
        soundBlock.events.forEach((event, soundEvent) -> legacy.put(event, soundEvent.nodes()));
        return legacy;
    }

    public synchronized Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("blocks", blocks.size());
        result.put("events", blocks.values().stream().mapToInt(block -> block.events.size()).sum());
        result.put("variants", totalVariants);
        result.put("playRequests", playRequests);
        result.put("playSuccesses", playSuccesses);
        result.put("playMisses", playMisses);
        return result;
    }

    public synchronized Map<String, Object> describeEvent(String block, String event) {
        SoundEvent soundEvent = findEvent(block, event).orElse(null);
        if (soundEvent == null) {
            Map<String, Object> missing = new LinkedHashMap<>();
            missing.put("ok", false);
            missing.put("block", block);
            missing.put("event", event);
            missing.put("available", false);
            return missing;
        }
        return soundEvent.toMap();
    }

    public synchronized List<Map<String, Object>> snapshot() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SoundBlock block : blocks.values()) {
            for (SoundEvent event : block.events.values()) {
                result.add(event.toMap());
            }
        }
        return result;
    }

    public AudioNode randomAudioNode(List<AudioNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        return nodes.get(random.nextInt(nodes.size()));
    }

    public static final class SoundBlock {
        private final String id;
        private final Map<String, SoundEvent> events = new LinkedHashMap<>();

        private SoundBlock(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public Map<String, SoundEvent> events() {
            return Collections.unmodifiableMap(events);
        }
    }

    public static final class SoundEvent {
        private final String block;
        private final String event;
        private final String packagePath;
        private final List<SoundVariant> variants;
        private final float cooldownSeconds;
        private long playCount;
        private long missCount;

        public SoundEvent(String block, String event, String packagePath, List<SoundVariant> variants, float cooldownSeconds) {
            this.block = block;
            this.event = event;
            this.packagePath = packagePath == null ? "" : packagePath;
            this.variants = new ArrayList<>(variants == null ? List.of() : variants);
            this.cooldownSeconds = Math.max(0f, cooldownSeconds);
        }

        public String block() {
            return block;
        }

        public String event() {
            return event;
        }

        public int size() {
            return variants.size();
        }

        public List<AudioNode> nodes() {
            List<AudioNode> nodes = new ArrayList<>(variants.size());
            for (SoundVariant variant : variants) {
                nodes.add(variant.node());
            }
            return nodes;
        }

        private AudioNode randomPlayable(Random random) {
            if (variants.isEmpty()) {
                missCount++;
                return null;
            }
            int start = random.nextInt(variants.size());
            for (int i = 0; i < variants.size(); i++) {
                SoundVariant variant = variants.get((start + i) % variants.size());
                if (variant.playable()) {
                    return variant.node();
                }
            }
            missCount++;
            return null;
        }

        private void markPlayed(AudioNode node) {
            for (SoundVariant variant : variants) {
                if (variant.node() == node) {
                    variant.cooldownUntilNanos = System.nanoTime() + Math.round(cooldownSeconds * 1_000_000_000.0);
                    variant.playCount++;
                    playCount++;
                    return;
                }
            }
        }

        private int indexOf(AudioNode node) {
            for (int i = 0; i < variants.size(); i++) {
                if (variants.get(i).node() == node) {
                    return i;
                }
            }
            return -1;
        }

        private String pathOf(AudioNode node) {
            for (SoundVariant variant : variants) {
                if (variant.node() == node) {
                    return variant.path();
                }
            }
            return "";
        }

        private void update(float tpf) {
            // Cooldowns are wall-clock based so provider update cadence cannot strand one-shot sounds.
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("ok", true);
            map.put("available", true);
            map.put("block", block);
            map.put("event", event);
            map.put("package", packagePath);
            map.put("variants", variants.size());
            map.put("cooldownSeconds", cooldownSeconds);
            map.put("playCount", playCount);
            map.put("missCount", missCount);
            List<Map<String, Object>> variantMaps = new ArrayList<>();
            for (int i = 0; i < variants.size(); i++) {
                variantMaps.add(variants.get(i).toMap(i));
            }
            map.put("variantList", variantMaps);
            return map;
        }
    }

    public static final class SoundVariant {
        private final String path;
        private final AudioNode node;
        private long cooldownUntilNanos;
        private long playCount;

        public SoundVariant(String path, AudioNode node) {
            this.path = path;
            this.node = node;
        }

        public String path() {
            return path;
        }

        public AudioNode node() {
            return node;
        }

        private boolean playable() {
            return cooldownRemaining() <= 0f;
        }

        private float cooldownRemaining() {
            return Math.max(0f, (cooldownUntilNanos - System.nanoTime()) / 1_000_000_000f);
        }

        private Map<String, Object> toMap(int index) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("index", index);
            map.put("path", path);
            map.put("status", node.getStatus().name());
            map.put("cooldownRemaining", cooldownRemaining());
            map.put("playCount", playCount);
            return map;
        }
    }

    public record PlayResult(
            boolean played,
            String block,
            String event,
            int variantIndex,
            int variants,
            String path,
            String reason
    ) {
        private static PlayResult played(String block, String event, int variantIndex, int variants, String path) {
            return new PlayResult(true, block, event, variantIndex, variants, path, "played");
        }

        private static PlayResult missed(String block, String event, String reason) {
            return new PlayResult(false, block, event, -1, 0, "", reason);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("played", played);
            map.put("block", block);
            map.put("event", event);
            map.put("variantIndex", variantIndex);
            map.put("variants", variants);
            map.put("path", path);
            map.put("reason", reason);
            return map;
        }
    }
}
