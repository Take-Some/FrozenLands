local M = {
  id = "engine.player",
  PLAYER_MANAGER_READY = "player.manager.ready",
  PLAYER_MANAGER_TELEMETRY = "player.manager.telemetry",
  PLAYER_ACTIVE_CHANGED = "player.active.changed",
  PLAYER_SPAWNED = "player.spawned",
  PLAYER_WARPED = "player.warped",
  PLAYER_FOOTSTEP = "player.footstep",
  PLAYER_TAKEOFF = "player.takeoff",
  PLAYER_LANDED = "player.landed",
  PLAYER_TOOL_EQUIPPED = "player.tool.equipped",
  PLAYER_TOOL_SWING = "player.tool.swing",
  PLAYER_TOOL_MISSED = "player.tool.missed",
  PLAYER_GRINDABLE_HIT = "player.grindable.hit",
  PLAYER_GRINDABLE_DESTROYED = "player.grindable.destroyed",
  PLAYER_TREE_HIT = "player.grindable.hit",
  PLAYER_TREE_CHOPPED = "player.grindable.destroyed",
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
