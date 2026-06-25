-- FrozenLands Lua API: engine.particles
-- Java is the host executor. This file is the Lua-side module facade.

local M = {
  id = "engine.particles",
  commands = {
    "status",
    "snow.enable",
    "snow.rate",
    "emit",
    "impact"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

function M.emit(effect, x, y, z)
  return M.call("emit", { effect = effect, x = x, y = y, z = z })
end

function M.impact(effect, x, y, z)
  return M.call("impact", { effect = effect, x = x, y = y, z = z })
end

return M
