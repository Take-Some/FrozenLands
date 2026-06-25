-- Lua-side representation of the built-in combobox UI element.
local M = {
    type = "combo",
    audio = {
        ["combo:open"] = {
            id = "ui.combo.open",
            source = "engine-ui/sounds/ui/combobox/open.ogg",
            bus = "ui",
            gain = 0.32,
            pitch = 1.0,
            pan = 0.0,
            looping = false,
            stopExisting = false,
            role = "ui"
        },

        ["combo:close"] = {
            id = "ui.combo.close",
            source = "engine-ui/sounds/ui/combobox/close.ogg",
            bus = "ui",
            gain = 0.30,
            pitch = 1.0,
            pan = 0.0,
            looping = false,
            stopExisting = false,
            role = "ui"
        },

        ["combo:select"] = {
            id = "ui.combo.select",
            source = "engine-ui/sounds/ui/combobox/select.ogg",
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

return M
