-- Lua hook: player.spawned

local core = require("engine.core")
local payload = args.payload or {}

core.emit("lua.player.spawned", {
  playerRef = payload.playerRef,
  reason = payload.reason,
  x = payload.x,
  y = payload.y,
  z = payload.z,
  caughtBy = "events/player_spawned.lua"
})

return {
  ok = true,
  caught = true,
  topic = args.topic,
  playerRef = payload.playerRef
}
