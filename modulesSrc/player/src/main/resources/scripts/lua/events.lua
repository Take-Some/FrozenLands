local M = {
  id = "engine.player",
  PLAYER_CAMERA_VIEW_TOGGLE_REQUESTED = "player.camera.view.toggle.requested",
  PLAYER_CAMERA_VIEW_REQUESTED = "player.camera.view.requested",
  PLAYER_CAMERA_VIEW_CHANGED = "player.camera.view.changed",
  LUA_PLAYER_CAMERA_VIEW_CHANGED = "lua.player.camera.view.changed",
  PLAYER_HEAD_TURN_REQUESTED = "player.head.turn.requested",
  PLAYER_HEAD_TURN_CHANGED = "player.head.turn.changed"
}

function M.on_event(event)
  return event
end

return M
