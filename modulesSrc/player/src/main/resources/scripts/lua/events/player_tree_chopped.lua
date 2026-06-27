-- Lua hook: player.tree.chopped

local core = require("engine.core")
local payload = args.payload or {}

core.emit("lua.player.tree.chopped", {
  playerRef = payload.playerRef,
  treeId = payload.treeId or payload.grindableId,
  tool = payload.tool,
  damage = payload.damage,
  x = payload.x,
  y = payload.y,
  z = payload.z,
  caughtBy = "events/player_tree_chopped.lua"
})

return {
  ok = true,
  caught = true,
  topic = args.topic,
  treeId = payload.treeId or payload.grindableId
}
