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
    "events.snapshot",
    "events.drain",
    "script.manifest",
    "script.list",
    "script.read",
    "script.run",
    "script.autorun",
    "console.execute"
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

function M.emit(topic, payload)
  return M.call("event.publish", { topic = topic, payload = payload or {} })
end

M.events = {}
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

return M
