-- FrozenLands Lua API: engine.sky

local M = {
  id = "engine.sky",
  commands = {
    "status",
    "command.execute",
    "atmosphere.setGradient",
    "weather.set",
    "weather.list",
    "clock.setTime",
    "environment.snapshot"
  },
  events = {
    command_queued = "sky.command.queued",
    transition_started = "sky.transition.started"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

function M.status()
  return M.call("status", {})
end

function M.command(command_id, args)
  return M.call("command.execute", { commandId = command_id, args = args or {} })
end

M.atmosphere = {}

function M.atmosphere.setGradient(style, seconds)
  return M.call("atmosphere.setGradient", { style = style, seconds = seconds or 0 })
end

M.weather = {}

function M.weather.set(id, seconds)
  local args = { id = id }
  if seconds ~= nil then
    args.seconds = seconds
  end
  return M.call("weather.set", args)
end

function M.weather.list()
  return M.call("weather.list", {})
end

M.clock = {}

function M.clock.setTime(hour)
  return M.call("clock.setTime", { hour = hour })
end

M.environment = {}

function M.environment.snapshot()
  return M.call("environment.snapshot", {})
end

M.atmosphere.set_gradient = M.atmosphere.setGradient
M.clock.set_time = M.clock.setTime

return M
