-- FrozenLands Lua API: engine.core
-- Core facade for module calls, events, scripting and console routing.

local M = {
  id = "engine.core",
  commands = {
    "status",
    "manifest",
    "modules",
    "providers",
    "call.module",
    "call.provider",
    "event.publish",
    "events.status",
    "events.topics",
    "events.latest",
    "events.recent",
    "events.snapshot",
    "events.drain",
    "script.manifest",
    "script.list",
    "script.read",
    "script.run",
    "script.autorun",
    "console.execute",
    "console.help",
    "console.version",
    "console.commandsList",
    "console.complete",
    "task.pool.status",
    "task.list",
    "task.get",
    "task.cancel",
    "service.pool.status",
    "service.list"
  }
}

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

function M.module(module_id)
  return {
    id = module_id,
    call = function(command, args)
      return M.call("call.module", { module = module_id, command = command, args = args or {} })
    end
  }
end

function M.provider(provider_id)
  return {
    id = provider_id,
    call = function(command, args)
      return M.call("call.provider", { provider = provider_id, command = command, args = args or {} })
    end
  }
end

function M.emit(topic, payload, options)
  options = options or {}
  options.topic = topic
  options.payload = payload or {}
  return M.call("event.publish", options)
end

function M.emit_live(topic, payload, options)
  options = options or {}
  options.live = true
  return M.emit(topic, payload, options)
end

M.events = {}
function M.events.status()
  return M.call("events.status", {})
end
function M.events.topics()
  return M.call("events.topics", {})
end
function M.events.latest(source, topic)
  return M.call("events.latest", { source = source or "all", topic = topic or "" })
end
function M.events.recent(limit, source)
  return M.call("events.recent", { limit = limit or 50, source = source or "all" })
end
function M.events.snapshot()
  return M.call("events.snapshot", {})
end
function M.events.drain()
  return M.call("events.drain", {})
end

M.script = {}
function M.script.manifest()
  return M.call("script.manifest", {})
end
function M.script.list()
  return M.call("script.list", {})
end
function M.script.read(script_name)
  return M.call("script.read", { script = script_name })
end
function M.script.run(script_name, run_args)
  run_args = run_args or {}
  run_args.script = script_name
  return M.call("script.run", run_args)
end
function M.script.autorun()
  return M.call("script.autorun", {})
end

M.console = {}
function M.console.execute(line)
  return M.call("console.execute", { line = line })
end
function M.console.run(line)
  return M.console.execute(line)
end
function M.console.help(command)
  return M.call("console.help", { command = command or "" })
end
function M.console.version()
  return M.call("console.version", {})
end
function M.console.commandsList()
  return M.call("console.commandsList", {})
end
function M.console.commands()
  return M.console.commandsList()
end
function M.console.complete(prefix)
  return M.call("console.complete", { prefix = prefix or "" })
end

M.services = {}
function M.services.status()
  return M.call("service.pool.status", {})
end
function M.services.list()
  return M.call("service.list", {})
end

return M
