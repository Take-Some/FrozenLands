package org.takesome.frozenlands.engine.world.effect;

import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.EngineContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParticleEffectRegistry {
    private final EngineContext engineContext;
    private final ParticleRuntimeSettings settings;
    private final List<ParticleBurstEffect> activeBursts = new ArrayList<>();
    private long emitted;
    private long expired;
    private long dropped;

    public ParticleEffectRegistry(EngineContext engineContext, ParticleRuntimeSettings settings) {
        this.engineContext = engineContext;
        this.settings = settings;
    }

    public Map<String, Object> emit(String effectId, Vector3f position, String eventTopic, String reason) {
        String resolvedId = normalizeEffectId(effectId);
        if (activeBursts.size() >= settings.maxActiveBursts()) {
            dropped++;
            Map<String, Object> result = eventPayload(resolvedId, position, reason);
            result.put("ok", false);
            result.put("dropped", true);
            result.put("reason", "max-active-bursts");
            engineContext.getModuleRegistry().publishEvent("particles.dropped", result);
            return result;
        }

        ParticleBurstEffect burst = new ParticleBurstEffect(engineContext, settings.effect(resolvedId), position);
        engineContext.getRootNode().attachChild(burst);
        activeBursts.add(burst);
        emitted++;
        burst.trigger();

        Map<String, Object> payload = eventPayload(resolvedId, position, reason);
        payload.put("ok", true);
        payload.put("activeBursts", activeBursts.size());
        payload.put("emitted", emitted);
        engineContext.getModuleRegistry().publishEvent(eventTopic, payload);
        return payload;
    }

    public void update(float tpf) {
        int before = activeBursts.size();
        activeBursts.removeIf(ParticleBurstEffect::isExpired);
        expired += Math.max(0, before - activeBursts.size());
    }

    public void cleanup() {
        activeBursts.forEach(ParticleBurstEffect::removeFromParent);
        activeBursts.clear();
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("activeBursts", activeBursts.size());
        status.put("maxActiveBursts", settings.maxActiveBursts());
        status.put("emitted", emitted);
        status.put("expired", expired);
        status.put("dropped", dropped);
        status.put("effects", settings.effectsManifest());
        status.put("effectIds", List.copyOf(settings.effectIds()));
        return status;
    }

    public Map<String, Object> effectStatus(String effectId) {
        ParticleRuntimeSettings.EffectSettings effect = settings.effect(normalizeEffectId(effectId));
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ok", true);
        status.put("id", effect.id());
        status.put("texture", effect.texture());
        status.put("particles", effect.particles());
        status.put("life", effect.life());
        status.put("startSize", effect.startSize());
        status.put("endSize", effect.endSize());
        status.put("velocityVariation", effect.velocityVariation());
        return status;
    }

    private String normalizeEffectId(String effectId) {
        if (effectId != null && !effectId.isBlank() && settings.effectIds().contains(effectId)) {
            return effectId;
        }
        return settings.defaultEffect();
    }

    private Map<String, Object> eventPayload(String effectId, Vector3f position, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("effect", effectId);
        payload.put("reason", reason == null ? "manual" : reason);
        payload.put("x", position.x);
        payload.put("y", position.y);
        payload.put("z", position.z);
        return payload;
    }
}
