package org.takesome.frozenlands.engine.player;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import org.takesome.frozenlands.engine.events.EngineEventTopics;
import org.takesome.frozenlands.engine.gameplay.GameplayUserData;
import org.takesome.frozenlands.engine.gameplay.GrindableCollisionProxyControl;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerGrindableInteractor {
    private final Player player;
    private final PlayerRuntimeSettings settings;

    public PlayerGrindableInteractor(Player player, PlayerRuntimeSettings settings) {
        this.player = player;
        this.settings = settings;
    }

    public void interactWithCameraTarget() {
        Camera camera = player.getCamera();
        if (camera == null) {
            return;
        }

        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(camera.getLocation(), camera.getDirection());
        player.getRootNode().collideWith(ray, results);

        for (int i = 0; i < results.size(); i++) {
            CollisionResult result = results.getCollision(i);
            if (result.getDistance() > settings.toolRange()) {
                break;
            }
            Spatial target = findTarget(result.getGeometry());
            if (target == null) {
                continue;
            }
            applyDamage(target, result);
            return;
        }

        player.publishEvent(settings.interactionMissedTopic(EngineEventTopics.PLAYER_TOOL_MISSED), Map.of(
                "playerRef", player.runtimeId(),
                "tool", settings.toolName(),
                "targetType", settings.interactionTargetType(),
                "range", settings.toolRange()
        ));
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("targetType", settings.interactionTargetType());
        status.put("hitTopic", settings.interactionHitTopic(EngineEventTopics.PLAYER_GRINDABLE_HIT));
        status.put("destroyedTopic", settings.interactionDestroyedTopic(EngineEventTopics.PLAYER_GRINDABLE_DESTROYED));
        status.put("missedTopic", settings.interactionMissedTopic(EngineEventTopics.PLAYER_TOOL_MISSED));
        return status;
    }

    private Spatial findTarget(Spatial spatial) {
        Spatial cursor = spatial;
        while (cursor != null) {
            if (Boolean.TRUE.equals(cursor.getUserData(GameplayUserData.GRINDABLE))
                    && Boolean.TRUE.equals(cursor.getUserData(GameplayUserData.GRINDABLE_MODEL))
                    && !Boolean.TRUE.equals(cursor.getUserData(GameplayUserData.GRINDABLE_DESTROYED))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private void applyDamage(Spatial target, CollisionResult hit) {
        float maxHealth = floatUserData(target, GameplayUserData.GRINDABLE_MAX_HEALTH, settings.grindableDefaultHealth());
        float health = floatUserData(target, GameplayUserData.GRINDABLE_HEALTH, maxHealth);
        float nextHealth = Math.max(0f, health - settings.toolDamage());
        target.setUserData(GameplayUserData.GRINDABLE_HEALTH, nextHealth);

        Map<String, Object> payload = targetPayload(target, nextHealth, maxHealth);
        payload.put("playerRef", player.runtimeId());
        payload.put("tool", settings.toolName());
        payload.put("targetType", settings.interactionTargetType());
        payload.put("damage", settings.toolDamage());
        payload.put("hitX", hit.getContactPoint().x);
        payload.put("hitY", hit.getContactPoint().y);
        payload.put("hitZ", hit.getContactPoint().z);
        player.publishEvent(settings.interactionHitTopic(EngineEventTopics.PLAYER_GRINDABLE_HIT), payload);

        if (nextHealth <= 0f) {
            destroyTarget(target, payload);
        }
    }

    private void destroyTarget(Spatial target, Map<String, Object> hitPayload) {
        target.setUserData(GameplayUserData.GRINDABLE_DESTROYED, true);
        RigidBodyControl control = target.getControl(RigidBodyControl.class);
        if (control != null) {
            player.getPhysicsSpace().remove(control);
            target.removeControl(control);
        }
        GrindableCollisionProxyControl proxyControl = target.getControl(GrindableCollisionProxyControl.class);
        if (proxyControl != null) {
            proxyControl.detachProxy();
            target.removeControl(proxyControl);
        }
        target.removeFromParent();

        Map<String, Object> destroyed = new LinkedHashMap<>(hitPayload);
        destroyed.put(settings.interactionDestroyedFlag(), true);
        player.publishEvent(settings.interactionDestroyedTopic(EngineEventTopics.PLAYER_GRINDABLE_DESTROYED), destroyed);
    }

    private Map<String, Object> targetPayload(Spatial target, float health, float maxHealth) {
        int kindId = intUserData(target, GameplayUserData.GRINDABLE_KIND, 0);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetId", intUserData(target, GameplayUserData.GRINDABLE_ID, System.identityHashCode(target)));
        payload.put("grindableId", payload.get("targetId"));
        payload.put("kindId", kindId);
        payload.put("kind", settings.grindableKindName(kindId));
        payload.put("health", health);
        payload.put("maxHealth", maxHealth);
        payload.put("x", target.getWorldTranslation().x);
        payload.put("y", target.getWorldTranslation().y);
        payload.put("z", target.getWorldTranslation().z);
        return payload;
    }

    private float floatUserData(Spatial spatial, String key, float fallback) {
        Object value = spatial.getUserData(key);
        return value instanceof Number number ? number.floatValue() : value == null ? fallback : Float.parseFloat(String.valueOf(value));
    }

    private int intUserData(Spatial spatial, String key, int fallback) {
        Object value = spatial.getUserData(key);
        return value instanceof Number number ? number.intValue() : value == null ? fallback : Integer.parseInt(String.valueOf(value));
    }
}
