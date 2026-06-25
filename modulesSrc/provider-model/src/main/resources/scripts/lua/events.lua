local M = { id = "engine.model" }

M.handlers = { ["provider.model.loaded"] = true, ["provider.model.detached"] = true }

function M.on_event(event)
  return event
end

