-- FrozenLands HtmlDom UI bridge.
-- HtmlDom calls these hooks around native action handling.

local ui = {
    screen = "hud"
}

local function set_attr(element, name, value)
    if element ~= nil and element.setAttribute ~= nil then
        element:setAttribute(name, value)
    end
end

local function root()
    if dom == nil or dom.getElementById == nil then
        return nil
    end
    return dom.getElementById("fl-root")
end

local function by_id(id)
    if dom == nil or dom.getElementById == nil then
        return nil
    end
    return dom.getElementById(id)
end

local function set_hidden(id, hidden)
    local element = by_id(id)
    if hidden then
        set_attr(element, "aria-hidden", "true")
    else
        set_attr(element, "aria-hidden", "false")
    end
end

local function mode_for_screen(name)
    if name == "main-menu" then
        return "boot"
    end
    if name == "pause" then
        return "paused"
    end
    return "gameplay"
end

local function set_screen(name)
    ui.screen = name
    local app = root()
    set_attr(app, "data-screen", name)
    set_attr(app, "data-mode", mode_for_screen(name))
    set_hidden("screen-hud", name ~= "hud")
    set_hidden("screen-pause", name ~= "pause")
    set_hidden("screen-main-menu", name ~= "main-menu")
end

function onClick(action, elementId)
    if action == "game.new" or action == "game.continue" or action == "game.resume" then
        set_screen("hud")
        return true
    end

    if action == "ui.pause" then
        set_screen("pause")
        return true
    end

    if action == "ui.mainMenu" then
        set_screen("main-menu")
        return true
    end

    if action == "ui.settings" then
        set_screen("pause")
        return true
    end

    if action == "ui.devtoolsHint" then
        return true
    end

    if string.sub(action, 1, 12) == "tool.select." then
        return true
    end

    return false
end

function afterClick(action, elementId)
    -- Native Java handling may run after Lua when onClick returns false.
end

function onTransitionEnd(propertyName, targetId, currentTargetId, elapsedMs)
    -- Reserved for UI audio/telemetry hooks.
end

return ui
