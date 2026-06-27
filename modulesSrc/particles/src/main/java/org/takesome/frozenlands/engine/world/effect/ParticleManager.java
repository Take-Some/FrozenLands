package org.takesome.frozenlands.engine.world.effect;

import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.events.EngineEventPayload;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.events.EventSubscriptionBag;
import org.takesome.frozenlands.engine.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ParticleManager {
    private final EngineContext engineContext;
    private final ParticleRuntimeSettings settings;
    private final ParticleEffectRegistry effectRegistry;
    private final EventSubscriptionBag subscriptions = new EventSubscriptionBag();
    private SnowfallEffect snowfallEffect;
    private boolean snowEnabled;
    private float snowRate;

    public ParticleManager(EngineContext engineContext) {
        this.engineContext = engineContext;
        this.settings = new ParticleRuntimeSettings();
        this.effectRegistry = new ParticleEffectRegistry(engineContext, settings);
        this.snowEnabled = settings.snowEnabled();
        this.snowRate = settings.snowRate();
    }

    public void initialize() {
        if (snowfallEffect == null) {
            snowfallEffect = new SnowfallEffect(engineContext, settings);
            snowfallEffect.setParticlesPerSec(snowEnabled ? snowRate : 0f);
            snowfallEffect.follow(engineContext.getCamera().getLocation());
            engineContext.getRootNode().attachChild(snowfallEffect);
            engineContext.getModuleRegistry().publishEvent("particles.snow.initialized", status());
        }
        subscribeGameplayEvents();
    }

    public void cleanup() {
        closeSubscriptions();
        if (snowfallEffect != null) {
            snowfallEffect.removeFromParent();
            snowfallEffect = null;
        }
        effectRegistry.cleanup();
    }

    public void update(float tpf) {
        updateSnow();
        effectRegistry.update(tpf);
    }

    public ParticleEffectRegistry effectRegistry() {
        return effectRegistry;
    }

    public void setSnowEnabled(boolean snowEnabled) {
        this.snowEnabled = snowEnabled;
        if (snowfallEffect != null) {
            snowfallEffect.setParticlesPerSec(snowEnabled ? snowRate : 0f);
        }
        engineContext.getModuleRegistry().publishEvent("particles.snow.enabled", status());
    }

    public void setSnowRate(float snowRate) {
        this.snowRate = Math.max(0f, snowRate);
        if (snowfallEffect != null && snowEnabled) {
            snowfallEffect.setParticlesPerSec(this.snowRate);
        }
        engineContext.getModuleRegistry().publishEvent("particles.snow.rate", status());
    }

    public Map<String, Object> emit(String effectId, Vector3f position) {
        return effectRegistry.emit(effectId, position, "particles.emitted", "manual.emit");
    }

    public Map<String, Object> impact(String effectId, Vector3f position) {
        return effectRegistry.emit(effectId, position, "particles.impact", "manual.impact");
    }

    public Map<String, Object> effectStatus(String effectId) {
        return effectRegistry.effectStatus(effectId);
    }

    public Map<String, Object> clearEffects() {
        effectRegistry.cleanup();
        engineContext.getModuleRegistry().publishEvent("particles.cleared", status());
        return status();
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ok", true);
        status.put("snowInitialized", snowfallEffect != null);
        status.put("snowEnabled", snowEnabled);
        status.put("snowRate", snowRate);
        status.put("eventBindingsEnabled", settings.eventBindingsEnabled());
        status.putAll(effectRegistry.status());
        if (snowfallEffect != null) {
            Vector3f position = snowfallEffect.getWorldTranslation();
            status.put("snowX", position.x);
            status.put("snowY", position.y);
            status.put("snowZ", position.z);
        }
        return status;
    }

    private void updateSnow() {
        if (snowfallEffect == null) {
            return;
        }
        Player player = engineContext.findService(Player.class).orElse(null);
        Vector3f followCenter = player != null
                ? player.getWorldTranslation()
                : engineContext.getCamera().getLocation();
        snowfallEffect.follow(followCenter);
    }

    private void subscribeGameplayEvents() {
        if (!subscriptions.isEmpty() || !settings.eventBindingsEnabled()) {
            return;
        }
        subscriptions.add(engineContext.getModuleRegistry().getEventBus().subscribe(
                EngineEventTopics.PARTICLE_EFFECT_REQUESTED,
                this::emitFromRequest));
    }

    private void emitFromRequest(Map<String, Object> event) {
        Map<String, Object> payload = EngineEventPayload.of(event);
        String effectId = EngineEventPayload.string(payload, "effect", settings.defaultEffect());
        String reason = EngineEventPayload.string(payload, "reason", EngineEventPayload.string(payload, "source", "event.request"));
        Vector3f position = new Vector3f(
                EngineEventPayload.floating(payload, "x", 0f),
                EngineEventPayload.floating(payload, "y", 0f),
                EngineEventPayload.floating(payload, "z", 0f)
        );
        effectRegistry.emit(effectId, position, "particles.event.emitted", reason);
    }

    private void closeSubscriptions() {
        subscriptions.close();
    }
}
