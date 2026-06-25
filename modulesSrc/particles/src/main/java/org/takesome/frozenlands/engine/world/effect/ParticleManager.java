package org.takesome.frozenlands.engine.world.effect;

import com.jme3.math.Vector3f;
import org.takesome.frozenlands.engine.EngineContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParticleManager {
    private final EngineContext engineContext;
    private final ParticleRuntimeSettings settings;
    private final List<ParticleBurstEffect> activeBursts = new ArrayList<>();
    private SnowfallEffect snowfallEffect;
    private boolean snowEnabled;
    private float snowRate;

    public ParticleManager(EngineContext engineContext) {
        this.engineContext = engineContext;
        this.settings = new ParticleRuntimeSettings();
        this.snowEnabled = settings.snowEnabled();
        this.snowRate = settings.snowRate();
    }

    public void initialize() {
        if (snowfallEffect != null) {
            return;
        }
        snowfallEffect = new SnowfallEffect(engineContext, settings);
        snowfallEffect.setParticlesPerSec(snowEnabled ? snowRate : 0f);
        snowfallEffect.follow(engineContext.getCamera().getLocation());
        engineContext.getRootNode().attachChild(snowfallEffect);
        engineContext.getModuleRegistry().publishEvent("particles.snow.initialized", status());
    }

    public void cleanup() {
        if (snowfallEffect != null) {
            snowfallEffect.removeFromParent();
            snowfallEffect = null;
        }
        activeBursts.forEach(ParticleBurstEffect::removeFromParent);
        activeBursts.clear();
    }

    public void update(float tpf) {
        updateSnow();
        activeBursts.removeIf(ParticleBurstEffect::isExpired);
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
        return burst(effectId, position, "particles.emitted");
    }

    public Map<String, Object> impact(String effectId, Vector3f position) {
        return burst(effectId, position, "particles.impact");
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ok", true);
        status.put("snowInitialized", snowfallEffect != null);
        status.put("snowEnabled", snowEnabled);
        status.put("snowRate", snowRate);
        status.put("activeBursts", activeBursts.size());
        status.put("effects", settings.effectsManifest());
        if (snowfallEffect != null) {
            Vector3f position = snowfallEffect.getWorldTranslation();
            status.put("x", position.x);
            status.put("y", position.y);
            status.put("z", position.z);
        }
        return status;
    }

    private void updateSnow() {
        if (snowfallEffect == null) {
            return;
        }
        Vector3f followCenter = engineContext.getPlayer() != null
                ? engineContext.getPlayer().getWorldTranslation()
                : engineContext.getCamera().getLocation();
        snowfallEffect.follow(followCenter);
    }

    private Map<String, Object> burst(String effectId, Vector3f position, String eventTopic) {
        ParticleBurstEffect burst = new ParticleBurstEffect(engineContext, settings.effect(effectId), position);
        engineContext.getRootNode().attachChild(burst);
        activeBursts.add(burst);
        burst.trigger();

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("effect", effectId);
        event.put("x", position.x);
        event.put("y", position.y);
        event.put("z", position.z);
        event.put("activeBursts", activeBursts.size());
        engineContext.getModuleRegistry().publishEvent(eventTopic, event);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.putAll(event);
        return result;
    }
}
