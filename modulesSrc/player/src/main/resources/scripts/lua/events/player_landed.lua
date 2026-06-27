-- Lua hook: player.landed

local core = require("engine.core")
local payload = args.payload or {}

core.emit("lua.player.landed", {
  playerRef = payload.playerRef,
  velocityY = payload.velocityY,
  caughtBy = "events/player_landed.lua"
})

return {
  ok = true,
  caught = true,
  topic = args.topic,
  playerRef = payload.playerRef
}
