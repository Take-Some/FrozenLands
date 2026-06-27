local M = { id = "engine.shaders" }

function M.on_event(event)
  return event
end

M.events = {
  "engine.shaders.pipeline.initialized",
  "engine.shaders.pipeline.enabled.changed",
  "engine.shaders.effect.enabled.changed",
  "engine.shaders.settings.changed"
}

return M
