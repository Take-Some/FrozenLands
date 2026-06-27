-- FrozenLands Lua API: engine.shaders
-- Java is the host executor. This file is the Lua-side module facade.

local M = {
  id = "engine.shaders",
  commands = {
    "status",
    "setEnabled",
    "effects",
    "effect.get",
    "effect.set",
    "shadowSettings",
    "shadowSettings.set"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

function M.enable_effect(id)
  return M.call("effect.set", { id = id, enabled = true })
end

function M.disable_effect(id)
  return M.call("effect.set", { id = id, enabled = false })
end

return M
