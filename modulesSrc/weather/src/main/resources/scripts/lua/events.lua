local M = {
  id = "engine.weather",
  WEATHER_STATE_CHANGED = "weather.state.changed",
  SNOW_STARTED = "weather.snow.started",
  SNOW_STOPPED = "weather.snow.stopped",
  SNOW_DENSITY_CHANGED = "weather.snow.density.changed",
  WIND_CHANGED = "weather.wind.changed"
}

function M.on_event(event)
  return event
end

return M
