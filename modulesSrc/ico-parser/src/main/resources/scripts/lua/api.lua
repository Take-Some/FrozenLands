-- FrozenLands Lua API: engine.icoParser
-- Java is the host executor. This file is the Lua-side module facade.

local M = {
  id = "engine.icoParser",
  commands = {
    "status",
    "inspect",
    "best"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

return M
