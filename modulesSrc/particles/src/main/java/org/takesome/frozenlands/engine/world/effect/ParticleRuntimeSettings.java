package org.takesome.frozenlands.engine.world.effect;

import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ParticleRuntimeSettings {
    private final LuaRuntimeConfig loader = new LuaRuntimeConfig();
    private final Map<String, Object> config = loader.read("engine.particles");
    private final Map<String, Object> snow = loader.map(config, "snow");
    private final Map<String, Object> effects = loader.map(config, "effects");

    public boolean snowEnabled() { return loader.bool(snow, "enabled", true); }
    public int snowParticles() { return loader.integer(snow, "particles", 7000); }
    public float snowRate() { return loader.floating(snow, "particlesPerSecond", 450f); }
    public float areaXZ() { return loader.floating(snow, "areaXZ", 220f); }
    public float areaHeight() { return loader.floating(snow, "areaHeight", 90f); }
    public float followHeight() { return loader.floating(snow, "followHeight", 55f); }
    public float startSize() { return loader.floating(snow, "startSize", 0.045f); }
    public float endSize() { return loader.floating(snow, "endSize", 0.09f); }
    public float lowLife() { return loader.floating(snow, "lowLife", 6f); }
    public float highLife() { return loader.floating(snow, "highLife", 14f); }
    public float gravityY() { return loader.floating(snow, "gravityY", -0.32f); }
    public float velocityX() { return loader.floating(snow, "velocityX", 0.7f); }
    public float velocityY() { return loader.floating(snow, "velocityY", -0.35f); }
    public float velocityZ() { return loader.floating(snow, "velocityZ", 0.2f); }
    public float velocityVariation() { return loader.floating(snow, "velocityVariation", 0.65f); }

    public EffectSettings effect(String id) {
        Map<String, Object> effect = loader.map(effects, id);
        if (effect.isEmpty()) {
            throw new IllegalArgumentException("Particle effect is not registered: " + id);
        }
        return new EffectSettings(id, effect, loader);
    }

    public Set<String> effectIds() {
        return Collections.unmodifiableSet(effects.keySet());
    }

    public Map<String, Object> effectsManifest() {
        Map<String, Object> manifest = new LinkedHashMap<>();
        effects.forEach((id, value) -> manifest.put(id, value));
        return manifest;
    }

    public static final class EffectSettings {
        private final String id;
        private final Map<String, Object> values;
        private final LuaRuntimeConfig loader;

        private EffectSettings(String id, Map<String, Object> values, LuaRuntimeConfig loader) {
            this.id = id;
            this.values = values;
            this.loader = loader;
        }

        public String id() { return id; }
        public String texture() { return loader.string(values, "texture", "textures/snowflake.png"); }
        public int particles() { return loader.integer(values, "particles", 48); }
        public float life() { return loader.floating(values, "life", 1f); }
        public float startSize() { return loader.floating(values, "startSize", 0.05f); }
        public float endSize() { return loader.floating(values, "endSize", 0.15f); }
        public float gravityY() { return loader.floating(values, "gravityY", 0f); }
        public float velocityX() { return loader.floating(values, "velocityX", 0f); }
        public float velocityY() { return loader.floating(values, "velocityY", 0.5f); }
        public float velocityZ() { return loader.floating(values, "velocityZ", 0f); }
        public float velocityVariation() { return loader.floating(values, "velocityVariation", 0.75f); }
    }
}
