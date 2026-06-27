local M = {}

M.id = "frozenlands.terrain"
M.abi = 1

function M.profile(t)
    t = t or {}
    t.abi = t.abi or M.abi
    return t
end

function M.call(command, args)
    if engine and engine.callModule then
        return engine.callModule(M.id, command, args or {})
    end
    return { ok = false, reason = "engine-bridge-unavailable", command = command }
end

function M.status()
    return M.call("status", {})
end

function M.sample(x, z, seed)
    return M.call("sample", { x = x or 0, z = z or 0, seed = seed or 0 })
end

return M
