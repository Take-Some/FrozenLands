-- FrozenLands Lua event API: engine.particles
local M = { id = "engine.particles" }

M.handlers = {
  ["particles.emitted"] = true,
  ["particles.impact"] = true,
  ["particles.snow.enabled"] = true,
  ["particles.snow.rate"] = true
}

function M.accepts(topic)
  return M.handlers[topic] == true
end

function M.on_event(event)
  return event
end

return M
