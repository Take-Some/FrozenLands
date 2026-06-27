local M = { id = "engine.sound" }

M.handlers = {
  ["provider.sound.loaded"] = true,
  ["provider.sound.play"] = true,
  ["provider.sound.play.failed"] = true,
  ["engine.sound.play.requested"] = true,
  ["engine.sound.played"] = true,
  ["engine.sound.play.failed"] = true
}

function M.on_event(event)
  return event
end

