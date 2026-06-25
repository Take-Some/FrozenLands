local M = {
  id = "engine.weather",
  commands = {
    "status",
    "snow_start",
    "snow_stop",
    "snow_light",
    "snow_blizzard",
    "wind"
  },
  events = {
    weather_state_changed = "weather.state.changed",
    snow_started = "weather.snow.started",
    snow_stopped = "weather.snow.stopped",
    snow_density_changed = "weather.snow.density.changed",
    wind_changed = "weather.wind.changed"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

function M.start_snow()
  return M.call("snow_start", {})
end

function M.stop_snow()
  return M.call("snow_stop", {})
end

function M.light_snow()
  return M.call("snow_light", {})
end

function M.blizzard()
  return M.call("snow_blizzard", {})
end

function M.wind(x, z)
  return M.call("wind", { x = x or 0, y = 0, z = z or 0 })
end

return M
