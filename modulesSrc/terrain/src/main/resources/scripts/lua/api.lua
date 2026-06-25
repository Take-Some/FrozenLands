-- FrozenLands Lua API: engine.terrain
-- Java is the host executor. This file is the Lua-side module facade.

local M = {
  id = "engine.terrain",
  commands = {
    "status",
    "chunks",
    "heightAt",
    "spawnLocation"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

return M
