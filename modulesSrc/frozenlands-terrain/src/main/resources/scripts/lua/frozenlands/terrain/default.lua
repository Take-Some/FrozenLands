return {
  id = "frozenlands.default",
  abi = 1,
  version = 2,
  chunk_size = 64,
  cell_size = 1.0,
  visual_snow_scale = 0.35,
  walkable_snow_scale = 0.18,
  max_snow_sink = 0.18,

  height = {
    { id="base",      frequency=0.018, amplitude=14.0, octaves=5, lacunarity=2.0, persistence=0.50, seed_salt=11 },
    { id="ridges",    frequency=0.055, amplitude= 5.0, octaves=4, lacunarity=2.1, persistence=0.45, seed_salt=21, ridged=true },
    { id="snowdrift", frequency=0.120, amplitude= 1.8, octaves=3, lacunarity=2.0, persistence=0.55, seed_salt=31 }
  },

  climate = {
    temperature_base = -28.0,
    temperature_amp = 12.0,
    temperature_frequency = 0.010,
    moisture_frequency = 0.030,
    wind_frequency = 0.045,
    ice_bias = 0.0,
    snow_bias = 0.0
  },

  biomes = {
    glacial_plain    = { max_height= 7.0, max_roughness=0.42, min_ice=0.20 },
    snow_dunes       = { max_height=16.0, min_snow=0.55 },
    frozen_lake      = { max_height= 2.0, min_ice=0.72 },
    cracked_ice      = { max_height= 3.5, min_ice=0.55, min_roughness=0.46 },
    black_rock_ridge = { min_height=15.0, min_roughness=0.52 },
    geothermal_field = { min_temperature=-12.0, max_ice=0.35 },
    ruin_field       = { min_height=4.0, max_height=12.0, ruin_chance=0.018 }
  },

  features = {
    { id="dead_tree",    biomes={"snow_dunes", "glacial_plain"}, density=0.006, min_spacing=5, seed_salt=101 },
    { id="ice_crack",    biomes={"cracked_ice", "frozen_lake"},   density=0.025, min_spacing=2, seed_salt=102 },
    { id="black_rock",   biomes={"black_rock_ridge"},              density=0.018, min_spacing=3, seed_salt=103 },
    { id="supply_cache", biomes={"ruin_field"},                    density=0.002, min_spacing=12, seed_salt=104 },
    { id="steam_vent",   biomes={"geothermal_field"},              density=0.010, min_spacing=6, seed_salt=105 }
  },

  flags = {
    walkable_max_roughness = 0.78,
    icy_min = 0.58,
    fragile_ice_min = 0.65,
    fragile_stability_max = 0.42,
    deep_snow_min = 0.45,
    steep_min_roughness = 0.70,
    warm_temperature_min = -12.0,
    blocked_roughness_min = 0.68
  }
}
