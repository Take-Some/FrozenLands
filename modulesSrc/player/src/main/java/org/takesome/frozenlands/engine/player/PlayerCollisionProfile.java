package org.takesome.frozenlands.engine.player;

import com.jme3.math.Vector3f;

public final class PlayerCollisionProfile {
    private final String source;
    private final float radius;
    private final float height;
    private final float mass;
    private final Vector3f boundsCenter;
    private final Vector3f boundsExtent;

    public PlayerCollisionProfile(String source, float radius, float height, float mass, Vector3f boundsCenter, Vector3f boundsExtent) {
        this.source = source;
        this.radius = radius;
        this.height = height;
        this.mass = mass;
        this.boundsCenter = boundsCenter == null ? Vector3f.ZERO.clone() : boundsCenter.clone();
        this.boundsExtent = boundsExtent == null ? Vector3f.ZERO.clone() : boundsExtent.clone();
    }

    public String source() { return source; }
    public float radius() { return radius; }
    public float height() { return height; }
    public float mass() { return mass; }
    public Vector3f boundsCenter() { return boundsCenter.clone(); }
    public Vector3f boundsExtent() { return boundsExtent.clone(); }
}
