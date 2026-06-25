-- Lua hook: catches Java player.camera.view.changed and re-emits a Lua-observed event.

local core = require("engine.core")

local payload = args.payload or {}
local from_view = tostring(payload.from or "")
local to_view = tostring(payload.to or payload.view or "")

print("[camera-view.lua] caught player camera view change: " .. from_view .. " -> " .. to_view)

core.emit("lua.player.camera.view.changed", {
  playerRef = payload.playerRef,
  from = payload.from,
  to = payload.to,
  view = payload.view,
  reason = payload.reason,
  visualVisible = payload.visualVisible,
  transitionSeconds = payload.transitionSeconds,
  caughtBy = "events/player_camera_view_changed.lua"
})

return {
  ok = true,
  caught = true,
  topic = args.topic,
  from = payload.from,
  to = payload.to,
  view = payload.view
}
