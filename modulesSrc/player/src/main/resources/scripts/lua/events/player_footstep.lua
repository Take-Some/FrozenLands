-- Lua hook: player.footstep
-- Java computes cadence and sound playback. Lua can observe or add gameplay reactions.

local core = require("engine.core")
local payload = args.payload or {}

core.emit("lua.player.footstep", {
  playerRef = payload.playerRef,
  stepIndex = payload.stepIndex,
  gait = payload.gait,
  state = payload.state,
  horizontalSpeed = payload.horizontalSpeed,
  currentSpeed = payload.currentSpeed,
  soundEvent = payload.soundEvent,
  soundPlayed = payload.soundPlayed,
  caughtBy = "events/player_footstep.lua"
})

return {
  ok = true,
  caught = true,
  topic = args.topic,
  gait = payload.gait,
  stepIndex = payload.stepIndex
}
