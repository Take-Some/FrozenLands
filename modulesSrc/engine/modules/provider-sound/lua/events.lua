local M = { id = "engine.sound" }

M.handlers = { ["provider.sound.loaded"] = true, ["provider.sound.play"] = true }

function M.on_event(event)
  return event
end

