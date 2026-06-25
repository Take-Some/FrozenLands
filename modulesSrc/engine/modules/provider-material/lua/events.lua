local M = { id = "engine.material" }

M.handlers = { ["provider.material.loaded"] = true }

function M.on_event(event)
  return event
end

