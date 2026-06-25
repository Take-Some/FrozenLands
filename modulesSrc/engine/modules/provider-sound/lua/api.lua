-- FrozenLands Lua API: engine.sound
-- Java is the host executor. This file is the Lua-side module facade.

local M = {
  id = "engine.sound",
  commands = {
    "load",
    "list.blocks",
    "list.events",
    "play"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

return M
