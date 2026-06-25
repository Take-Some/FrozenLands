-- FrozenLands Lua API: engine.player
-- Java is the host executor. This file is the Lua-side module facade.

local M = {
  id = "engine.player",
  commands = {
    "status",
    "position",
    "warp"
  },
  events = {
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
