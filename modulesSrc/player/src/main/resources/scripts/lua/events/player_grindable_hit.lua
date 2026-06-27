-- Lua hook: player.grindable.hit

local core = require("engine.core")
local payload = args.payload or {}

core.emit("lua.player.grindable.hit", {
  playerRef = payload.playerRef,
  grindableId = payload.grindableId,
  kind = payload.kind,
  kindId = payload.kindId,
  tool = payload.tool,
  damage = payload.damage,
  health = payload.health,
  maxHealth = payload.maxHealth,
  x = payload.x,
  y = payload.y,
  z = payload.z,
  caughtBy = "events/player_grindable_hit.lua"
})

if payload.kind == "tree" or payload.kindId == 1 then
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
    caughtBy = "events/player_grindable_hit.lua"
  })
end

return {
  ok = true,
  caught = true,
  topic = args.topic,
  grindableId = payload.grindableId,
  kind = payload.kind,
  health = payload.health
}
