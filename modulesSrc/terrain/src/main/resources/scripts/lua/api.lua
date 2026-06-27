-- FrozenLands Lua API: engine.terrain
-- Java remains the hot path. Lua owns ABI-facing config, inspection and placement validation.

local M = {
  id = "engine.terrain",
  commands = {
    "status",
    "settings",
    "placementGroups",
    "validatePlacement",
    "chunks",
    "heightAt",
    "sample",
    "spawnLocation"
  }
}

local function call_bridge(command, args)
  args = args or {}
  if java and java.callModule then
    return java.callModule(M.id, command, args)
  end
  if engine and engine.callModule then
    return engine.callModule(M.id, command, args)
  end
  return { ok = false, error = "engine-bridge-unavailable", command = command }
end

function M.call(command, args)
  return call_bridge(command, args)
end

function M.status()
  return M.call("status", {})
end

function M.settings()
  return M.call("settings", {})
end

function M.placement_groups()
  return M.call("placementGroups", {})
end

function M.validate_placement(args)
  return M.call("validatePlacement", args or {})
end

function M.height_at(x, z)
  return M.call("heightAt", { x = x or 0, z = z or 0 })
end

function M.sample(x, z)
  return M.call("sample", { x = x or 0, z = z or 0 })
end

function M.spawn_location(x, z, clearance)
  return M.call("spawnLocation", { x = x or 0, z = z or 0, clearance = clearance or 4 })
end

return M
