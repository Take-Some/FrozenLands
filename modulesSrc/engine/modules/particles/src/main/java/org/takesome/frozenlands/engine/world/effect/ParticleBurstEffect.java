package org.takesome.frozenlands.engine.world.effect;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import org.takesome.frozenlands.engine.EngineContext;

public final class ParticleBurstEffect extends ParticleEmitter {
    private boolean expired;

    public ParticleBurstEffect(EngineContext context, ParticleRuntimeSettings.EffectSettings settings, Vector3f position) {
        super(settings.id(), ParticleMesh.Type.Triangle, settings.particles());
        Material material = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
        material.setTexture("Texture", context.getAssetManager().loadTexture(settings.texture()));
        setMaterial(material);
        setImagesX(1);
        setImagesY(1);
        setStartColor(new ColorRGBA(1f, 1f, 1f, 0.85f));
        setEndColor(new ColorRGBA(1f, 1f, 1f, 0f));
        setStartSize(settings.startSize());
        setEndSize(settings.endSize());
        setGravity(0, settings.gravityY(), 0);
        setLowLife(settings.life());
        setHighLife(settings.life());
        setParticlesPerSec(0);
        getParticleInfluencer().setInitialVelocity(new Vector3f(settings.velocityX(), settings.velocityY(), settings.velocityZ()));
        getParticleInfluencer().setVelocityVariation(settings.velocityVariation());
        setLocalTranslation(position);
        addControl(new ExpireControl(settings.life() + 0.25f));
    }

    public void trigger() {
        emitAllParticles();
    }

    public boolean isExpired() {
        return expired;
    }

    private final class ExpireControl extends AbstractControl {
        private final float ttl;
        private float age;

        private ExpireControl(float ttl) {
            this.ttl = ttl;
        }

        @Override
        protected void controlUpdate(float tpf) {
            age += tpf;
            if (age >= ttl) {
                expired = true;
                spatial.removeFromParent();
                spatial.removeControl(this);
            }
        }

        @Override
        protected void controlRender(RenderManager renderManager, ViewPort viewPort) {
        }
    }
}
