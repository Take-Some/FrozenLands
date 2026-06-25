-- FrozenLands Lua API: engine.world
-- Java is the host executor. This file is the Lua-side module facade.

local M = {
  id = "engine.world",
  commands = {
    "status",
    "spawnPlayer"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

return M
