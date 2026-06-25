-- FrozenLands Lua API: engine.shaders
-- Java is the host executor. This file is the Lua-side module facade.

local M = {
  id = "engine.shaders",
  commands = {
    "status",
    "setEnabled",
    "shadowSettings",
    "shadowSettings.set"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

return M
