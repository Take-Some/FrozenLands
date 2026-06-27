-- Lua hook: player.grindable.destroyed

local core = require("engine.core")
local payload = args.payload or {}

core.emit("lua.player.grindable.destroyed", {
  playerRef = payload.playerRef,
  grindableId = payload.grindableId,
  kind = payload.kind,
  kindId = payload.kindId,
  tool = payload.tool,
  damage = payload.damage,
  x = payload.x,
  y = payload.y,
  z = payload.z,
  caughtBy = "events/player_grindable_destroyed.lua"
})

if payload.kind == "tree" or payload.kindId == 1 then
  core.emit("lua.player.tree.chopped", {
    playerRef = payload.playerRef,
    treeId = payload.treeId or payload.grindableId,
    tool = payload.tool,
    damage = payload.damage,
    x = payload.x,
    y = payload.y,
    z = payload.z,
    caughtBy = "events/player_grindable_destroyed.lua"
  })
end

return {
  ok = true,
  caught = true,
  topic = args.topic,
  grindableId = payload.grindableId,
  kind = payload.kind
}
