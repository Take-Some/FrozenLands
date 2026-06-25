package org.takesome.frozenlands.engine.weather.snow;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import java.util.Random;

public final class Snowflake {
    public final Vector3f position = new Vector3f();
    public final Vector3f velocity = new Vector3f();

    public float size;
    public float alpha;
    public float rotation;
    public float angularVelocity;
    public float life;
    public float maxLife;

    public void reset(Random random, Vector3f anchor, SnowPrecipitationSystem.Config config) {
        float halfWidth = config.spawnWidth() * 0.5f;
        float halfDepth = config.spawnDepth() * 0.5f;

        position.set(
                anchor.x + randomRange(random, -halfWidth, halfWidth),
                anchor.y + randomRange(random, config.spawnMinHeight(), config.spawnMaxHeight()),
                anchor.z + randomRange(random, -halfDepth, halfDepth)
        );

        velocity.set(
                randomRange(random, -config.drift(), config.drift()),
                -randomRange(random, config.minFallSpeed(), config.maxFallSpeed()),
                randomRange(random, -config.drift(), config.drift())
        );

        size = randomRange(random, config.minSize(), config.maxSize());
        alpha = randomRange(random, config.minAlpha(), config.maxAlpha());
        rotation = random.nextFloat() * FastMath.TWO_PI;
        angularVelocity = randomRange(random, -config.maxAngularVelocity(), config.maxAngularVelocity());
        life = 0f;
        maxLife = randomRange(random, config.minLife(), config.maxLife());
    }

    public boolean update(float tpf, Vector3f wind, float killY) {
        life += tpf;

        velocity.x += wind.x * tpf;
        velocity.z += wind.z * tpf;

        position.x += velocity.x * tpf;
        position.y += velocity.y * tpf;
        position.z += velocity.z * tpf;
        rotation += angularVelocity * tpf;

        return life < maxLife && position.y > killY;
    }

    public float visibleAlpha() {
        if (maxLife <= 0f) {
            return alpha;
        }
        float normalized = life / maxLife;
        float fadeIn = Math.min(1f, normalized * 5f);
        float fadeOut = Math.min(1f, (1f - normalized) * 5f);
        return alpha * Math.max(0f, Math.min(fadeIn, fadeOut));
    }

    private static float randomRange(Random random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
