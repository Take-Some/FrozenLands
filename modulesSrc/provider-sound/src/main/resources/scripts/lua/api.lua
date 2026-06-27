-- FrozenLands Lua API: engine.sound
-- Java is the host executor. This file is the Lua-side module facade.

local M = {
  id = "engine.sound",
  commands = {
    "status",
    "load",
    "reload",
    "list.blocks",
    "list.events",
    "event.get",
    "registry.snapshot",
    "request",
    "play"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

function M.status()
  return M.call("status", {})
end

function M.reload()
  return M.call("reload", {})
end

function M.request(block, event, source)
  return M.call("request", { block = block or "player", event = event, source = source or "lua" })
end

function M.play(block, event)
  return M.call("play", { block = block or "player", event = event })
end

function M.events(block)
  return M.call("list.events", { block = block or "player" })
end

function M.event(block, event)
  return M.call("event.get", { block = block or "player", event = event })
end

return M
