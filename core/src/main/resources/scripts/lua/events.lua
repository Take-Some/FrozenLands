-- FrozenLands Lua event API: engine.core
local M = { id = "engine.core" }

M.handlers = {
  ["module.registered"] = true,
  ["module.command.executed"] = true,
  ["core.script.run.requested"] = true,
  ["core.script.loaded"] = true,
  ["core.script.executed"] = true,
  ["core.script.failed"] = true,
  ["core.console.executed"] = true
}

function M.accepts(topic)
  return M.handlers[topic] == true
end

function M.on_event(event)
  return event
end

return M
