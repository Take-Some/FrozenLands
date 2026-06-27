package org.takesome.frozenlands.engine.player;

import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventPayload;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.events.EventSubscriptionBag;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerFeedbackRouter implements AutoCloseable {
    private final EngineContext context;
    private final PlayerRuntimeSettings settings = new PlayerRuntimeSettings();
    private final EventSubscriptionBag subscriptions = new EventSubscriptionBag();
    private long eventsHandled;
    private long soundRequests;
    private long particleRequests;
    private String lastTopic = "";

    public PlayerFeedbackRouter(EngineContext context) {
        this.context = context;
    }

    public void start() {
        if (!subscriptions.isEmpty() || !settings.feedbackEnabled()) {
            return;
        }
        subscribe(EngineEventTopics.PLAYER_SPAWNED, this::onPlayerSpawned);
        subscribe(EngineEventTopics.PLAYER_FOOTSTEP, this::onFootstep);
        subscribe(EngineEventTopics.PLAYER_TAKEOFF, this::onTakeoff);
        subscribe(EngineEventTopics.PLAYER_LANDED, this::onLanded);
        subscribe(EngineEventTopics.PLAYER_GRINDABLE_HIT, this::onGrindableHit);
        subscribe(EngineEventTopics.PLAYER_GRINDABLE_DESTROYED, this::onGrindableDestroyed);
        context.getLogger().info("PlayerFeedbackRouter started subscriptions={}", subscriptions.size());
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ok", true);
        status.put("enabled", settings.feedbackEnabled());
        status.put("soundEnabled", settings.feedbackSoundEnabled());
        status.put("particleEnabled", settings.feedbackParticleEnabled());
        status.put("subscriptions", subscriptions.size());
        status.put("eventsHandled", eventsHandled);
        status.put("soundRequests", soundRequests);
        status.put("particleRequests", particleRequests);
        status.put("lastTopic", lastTopic);
        return status;
    }

    @Override
    public void close() {
        subscriptions.close();
    }

    private void subscribe(String topic, java.util.function.Consumer<Map<String, Object>> handler) {
        subscriptions.add(context.getModuleRegistry().getEventBus().subscribe(topic, event -> {
            eventsHandled++;
            lastTopic = topic;
            handler.accept(event);
        }));
    }

    private void onPlayerSpawned(Map<String, Object> event) {
        requestSound(settings.feedbackSound("spawn", "spawn"), "feedback.player.spawned", EngineEventPayload.of(event));
    }

    private void onFootstep(Map<String, Object> event) {
        Map<String, Object> payload = EngineEventPayload.of(event);
        if (!EngineEventPayload.bool(payload, "soundRequested", true)) {
            return;
        }
        String gait = EngineEventPayload.string(payload, "gait", "walking");
        String fallback = "sprinting".equals(gait) ? "sprinting" : "walking";
        String sound = settings.feedbackSound("footstep." + gait, EngineEventPayload.string(payload, "soundEvent", fallback));
        requestSound(sound, "feedback.player.footstep", payload);
    }

    private void onTakeoff(Map<String, Object> event) {
        requestSound(settings.feedbackSound("takeoff", "jump/takeoff"), "feedback.player.takeoff", EngineEventPayload.of(event));
    }

    private void onLanded(Map<String, Object> event) {
        Map<String, Object> payload = withPosition(EngineEventPayload.of(event));
        requestSound(settings.feedbackSound("landed", "jump/land"), "feedback.player.landed", payload);
        requestParticle(settings.feedbackParticle("landed", "snowImpact"), "feedback.player.landed", payload, "x", "y", "z");
    }

    private void onGrindableHit(Map<String, Object> event) {
        Map<String, Object> payload = EngineEventPayload.of(event);
        requestSound(settings.feedbackSound("grindable.hit", "chop"), "feedback.grindable.hit", payload);
        requestParticle(settings.feedbackParticle("grindable.hit", "woodChop"), "feedback.grindable.hit", payload, "hitX", "hitY", "hitZ");
    }

    private void onGrindableDestroyed(Map<String, Object> event) {
        Map<String, Object> payload = EngineEventPayload.of(event);
        requestParticle(settings.feedbackParticle("grindable.destroyed", "woodBreak"), "feedback.grindable.destroyed", payload, "hitX", "hitY", "hitZ");
    }

    private void requestSound(String sound, String source, Map<String, Object> originalPayload) {
        if (!settings.feedbackSoundEnabled() || sound == null || sound.isBlank()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("block", "player");
        payload.put("event", sound);
        payload.put("source", source);
        payload.put("playerRef", originalPayload.getOrDefault("playerRef", 0));
        payload.put("origin", EngineEventPayload.copy(originalPayload));
        context.getModuleRegistry().publishEvent(EngineEventTopics.ENGINE_SOUND_PLAY_REQUESTED, payload);
        soundRequests++;
    }

    private void requestParticle(String effect, String source, Map<String, Object> originalPayload, String xKey, String yKey, String zKey) {
        if (!settings.feedbackParticleEnabled() || effect == null || effect.isBlank()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("effect", effect);
        payload.put("source", source);
        payload.put("reason", source);
        payload.put("playerRef", originalPayload.getOrDefault("playerRef", 0));
        payload.put("x", EngineEventPayload.floating(originalPayload, xKey, EngineEventPayload.floating(originalPayload, "x", 0f)));
        payload.put("y", EngineEventPayload.floating(originalPayload, yKey, EngineEventPayload.floating(originalPayload, "y", 0f)));
        payload.put("z", EngineEventPayload.floating(originalPayload, zKey, EngineEventPayload.floating(originalPayload, "z", 0f)));
        payload.put("origin", EngineEventPayload.copy(originalPayload));
        context.getModuleRegistry().publishEvent(EngineEventTopics.PARTICLE_EFFECT_REQUESTED, payload);
        particleRequests++;
    }

    private Map<String, Object> withPosition(Map<String, Object> payload) {
        if (payload.containsKey("x") && payload.containsKey("y") && payload.containsKey("z")) {
            return payload;
        }
        Player player = context.findService(Player.class).orElse(null);
        if (player == null) {
            return payload;
        }
        Vector3f position = player.getWorldTranslation();
        Map<String, Object> enriched = new LinkedHashMap<>(payload);
        enriched.put("x", position.x);
        enriched.put("y", position.y);
        enriched.put("z", position.z);
        return enriched;
    }
}
