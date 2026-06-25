-- Lua-side representation of the built-in slider UI element.
local M = {
    type = "slider",
    audio = {
        ["slider:commit"] = {
            id = "ui.slider.commit",
            source = "engine-ui/sounds/ui/slider/commit.ogg",
            bus = "ui",
            gain = 0.30,
            pitch = 1.0,
            pan = 0.0,
            looping = false,
            stopExisting = false,
            role = "ui"
        }
    }
}

return M
