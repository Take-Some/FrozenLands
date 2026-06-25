-- Lua-side representation of the built-in checkbox UI element.
local M = {
    type = "checkbox",
    audio = {
        ["checkbox:changed"] = {
            id = "ui.checkbox.changed",
            sourceOn = "engine-ui/sounds/ui/checkbox/on.ogg",
            sourceOff = "engine-ui/sounds/ui/checkbox/off.ogg",
            bus = "ui",
            gain = 0.34,
            pitch = 1.0,
            pan = 0.0,
            looping = false,
            stopExisting = false,
            role = "ui"
        }
    }
}

function M.request(event, spec, ctx)
    local request = ctx.copySpec(spec)
    request.source = event.bool("checked") and spec.sourceOn or spec.sourceOff
    return request
end

return M
