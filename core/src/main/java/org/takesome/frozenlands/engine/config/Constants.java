package org.takesome.frozenlands.engine.config;

import com.jme3.math.Vector3f;
import com.jme3.post.ssao.SSAOFilter;
import org.takesome.frozenlands.engine.lua.LuaRuntimeConfig;

import java.util.Map;
import java.util.logging.Level;

public class Constants {
    private static final LuaRuntimeConfig LUA = new LuaRuntimeConfig();
    private static final Map<String, Object> WORLD = LUA.read("engine.world");
    private static final Map<String, Object> WORLD_SPAWN = LUA.map(WORLD, "spawn");
    private static final Map<String, Object> TERRAIN = LUA.read("engine.terrain");
    private static final Map<String, Object> TERRAIN_GRID = LUA.map(TERRAIN, "grid");
    private static final Map<String, Object> TERRAIN_SCALE = LUA.map(TERRAIN, "scale");
    private static final Map<String, Object> TERRAIN_LOD = LUA.map(TERRAIN, "lod");
    private static final Map<String, Object> TERRAIN_MOUNTAINS = LUA.map(TERRAIN, "mountains");
    private static final Map<String, Object> PARTICLES = LUA.read("engine.particles");
    private static final Map<String, Object> SNOW = LUA.map(PARTICLES, "snow");

    public static final Vector3f PLAYER_START_LOCATION = new Vector3f(
            LUA.floating(WORLD_SPAWN, "fallbackX", 0f),
            LUA.floating(WORLD_SPAWN, "fallbackY", 150f),
            LUA.floating(WORLD_SPAWN, "fallbackZ", 0f));

    public static final float DAMAGE = 25f;
    public static final int NPC_AMOUNT = 1;
    public static final int NPC_LOCATION_RANGE = 250;
    public static final int WATER_LEVEL_HEIGHT = 0;
    public static final int MODEL_ADJUSTMENT = 3;
    public static final float SOUND_VOLUME = 3f;
    public static final int MUSIC_VOLUME_MULTIPLIER = 6;
    public static final float SOUND_PITCH = 0.5f;
    public static final SSAOFilter SSAO_FILTER_BASIC = new SSAOFilter(1f, 1.5f, 5.8f, 0.9f);
    public static final String DEBUG = "Debug";
    public static final String POM_XML = "pom.xml";
    public static final String ROOT_LOGGER = "";
    public static final Level LOGGING_LEVEL = Level.INFO;
    public static final int MIN_FRAME_RATE = 30;

    public static final int MOUNTAINS_HEIGHT_OFFSET = Math.round(LUA.floating(TERRAIN_MOUNTAINS, "heightOffset", 50f));
    public static final int TERRAIN_PATCH_SIZE = LUA.integer(TERRAIN_GRID, "patchSize", 65);
    public static final int TERRAIN_QUAD_SIZE = LUA.integer(TERRAIN_GRID, "quadSize", 257);
    public static final float TERRAIN_TILE_NOISE_SCALE = LUA.floating(TERRAIN_GRID, "tileNoiseScale", 96f);
    public static final int TERRAIN_LOD_PATCH_SIZE = LUA.integer(TERRAIN_LOD, "patchSize", 129);
    public static final float TERRAIN_LOD_MULTIPLIER = LUA.floating(TERRAIN_LOD, "multiplier", 2.2f);
    public static final float TERRAIN_SCALE_X = LUA.floating(TERRAIN_SCALE, "x", 1.35f);
    public static final float TERRAIN_SCALE_Y = LUA.floating(TERRAIN_SCALE, "y", 1f);
    public static final float TERRAIN_SCALE_Z = LUA.floating(TERRAIN_SCALE, "z", 1.35f);

    public static final float SNOW_AREA_XZ = LUA.floating(SNOW, "areaXZ", 220f);
    public static final float SNOW_AREA_HEIGHT = LUA.floating(SNOW, "areaHeight", 90f);
    public static final float SNOW_FOLLOW_HEIGHT = LUA.floating(SNOW, "followHeight", 55f);

    public static final Vector3f RAY_DOWN = new Vector3f(0, -1, 0);
    public static final int HIT_PROBABILITY = 50;
    public static final float SHOOT_DELAY = 3f;
    public static final float SHOOT_RATE = 3.5f;
    public static final int STARS_COUNT = 500;
    public static final int DISTANCE_TO_STARS = 9000;
    public static final String VERSION_PROPERTIES = "version.properties";
    public static final int STENCIL_BITS = 8;
    public static final int RIGID_BODIES_SIZE = 4;
    public static float MELEE_DISTANCE_LIMIT = 15f;

    private Constants() {
    }
}
