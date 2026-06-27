-- Lua hook: player.tree.hit

local core = require("engine.core")
local payload = args.payload or {}

core.emit("lua.player.tree.hit", {
  playerRef = payload.playerRef,
  treeId = payload.treeId or payload.grindableId,
  tool = payload.tool,
  damage = payload.damage,
  health = payload.health,
  maxHealth = payload.maxHealth,
  x = payload.x,
  y = payload.y,
  z = payload.z,
  caughtBy = "events/player_tree_hit.lua"
})

return {
  ok = true,
  caught = true,
  topic = args.topic,
  treeId = payload.treeId or payload.grindableId,
  health = payload.health
}
