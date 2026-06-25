-- FrozenLands Lua API: engine.material
-- Java is the host executor. This file is the Lua-side module facade.

local M = {
  id = "engine.material",
  commands = {
    "load",
    "list",
    "get"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

return M
