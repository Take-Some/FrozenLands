-- Lua-side representation of the built-in button UI element.
local M = {
    type = "button",
    audio = {
        ["button:hover"] = {
            id = "ui.button.hover",
            source = "engine-ui/sounds/ui/button/hover.ogg",
            bus = "ui",
            gain = 0.28,
            pitch = 1.0,
            pan = 0.0,
            looping = false,
            stopExisting = false,
            role = "ui"
        },

        ["button:press"] = {
            id = "ui.button.press",
            source = "engine-ui/sounds/ui/button/press.ogg",
            bus = "ui",
            gain = 0.36,
            pitch = 1.0,
            pan = 0.0,
            looping = false,
            stopExisting = false,
            role = "ui"
        },

        ["button:submit"] = {
            id = "ui.button.submit",
            source = "engine-ui/sounds/ui/button/submit.ogg",
            bus = "ui",
            gain = 0.38,
            pitch = 1.0,
            pan = 0.0,
            looping = false,
            stopExisting = false,
            role = "ui"
        },

        ["button:reset"] = {
            id = "ui.button.reset",
            source = "engine-ui/sounds/ui/button/reset.ogg",
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

local DEFAULT_ACTIVATION_SOUND_KEY = "button:press"

local activationEvents = {
    ["button:click"] = true
}

local buttonTypeAliases = {
    [""] = DEFAULT_ACTIVATION_SOUND_KEY,
    ["button"] = DEFAULT_ACTIVATION_SOUND_KEY
}

local function firstButtonType(event, ctx)
    local value = ctx.lower(ctx.text(event, "type"))
    if value ~= "" then
        return value
    end
    value = ctx.lower(ctx.text(event, "button-type"))
    if value ~= "" then
        return value
    end
    return ctx.lower(ctx.text(event, "buttonType"))
end

local function activationSoundKey(event, ctx)
    local buttonType = firstButtonType(event, ctx)
    local alias = buttonTypeAliases[buttonType]
    if alias ~= nil then
        return alias
    end

    local typedKey = "button:" .. buttonType
    if M.audio[typedKey] ~= nil then
        return typedKey
    end

    return DEFAULT_ACTIVATION_SOUND_KEY
end

function M.soundKey(event, ctx)
    local key = ctx.eventKey(event)
    if activationEvents[key] then
        return activationSoundKey(event, ctx)
    end
    return key
end

function M.edgeEnabled(event, ctx)
    local key = ctx.eventKey(event)
    if key == "button:press" then
        return false
    end
    local interaction = ctx.text(event, "interaction")
    if interaction == "hover" then
        return event.bool("value")
    end
    return true
end

return M
