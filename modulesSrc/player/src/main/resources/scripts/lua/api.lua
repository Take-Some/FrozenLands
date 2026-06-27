-- FrozenLands Lua API: engine.player
-- Java owns runtime physics. Lua orchestrates player commands and reacts to player events.

local M = {
  id = "engine.player",
  commands = {
    "status",
    "manager.status",
    "position",
    "locomotion",
    "tool.status",
    "feedback.status",
    "warp"
  },
  events = {
    manager_ready = "player.manager.ready",
    manager_telemetry = "player.manager.telemetry",
    active_changed = "player.active.changed",
    spawned = "player.spawned",
    warped = "player.warped",
    footstep = "player.footstep",
    takeoff = "player.takeoff",
    landed = "player.landed",
    tool_equipped = "player.tool.equipped",
    tool_swing = "player.tool.swing",
    tool_missed = "player.tool.missed",
    grindable_hit = "player.grindable.hit",
    grindable_destroyed = "player.grindable.destroyed",
    tree_hit = "player.grindable.hit",
    tree_chopped = "player.grindable.destroyed",
    camera_view_toggle_requested = "player.camera.view.toggle.requested",
    camera_view_requested = "player.camera.view.requested",
    camera_view_changed = "player.camera.view.changed",
    lua_camera_view_changed = "lua.player.camera.view.changed",
    head_turn_requested = "player.head.turn.requested",
    head_turn_changed = "player.head.turn.changed"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

function M.status()
  return M.call("status", {})
end

function M.position()
  return M.call("position", {})
end

function M.locomotion()
  return M.call("locomotion", {})
end

function M.tool_status()
  return M.call("tool.status", {})
end

function M.warp(x, y, z, reason)
  return M.call("warp", { x = x, y = y, z = z, reason = reason or "lua" })
end

function M.toggle_view()
  return java.publishEvent(M.events.camera_view_toggle_requested, {})
end

function M.request_view(view)
  return java.publishEvent(M.events.camera_view_requested, { view = view })
end

function M.request_head_turn(yaw, pitch)
  return java.publishEvent(M.events.head_turn_requested, { yaw = yaw or 0, pitch = pitch or 0 })
end

return M
