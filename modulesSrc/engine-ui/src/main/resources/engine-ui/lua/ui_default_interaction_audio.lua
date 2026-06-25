-- Default UI feedback audio is routed from retained component events.
-- Element-specific audio policy lives in engine-ui/lua/ui_elements/*.lua.
local M = {}

local installed = false
local disabled = {}
local sounds = {}
local elements = {}

local lastKey = ""
local lastAt = 0.0
local dedupeSeconds = 0.09

local function text(event, key)
    if event == nil then
        return ""
    end
    local value = event.text(key)
    if value == nil then
        return ""
    end
    return tostring(value)
end

local function lower(value)
    return string.lower(tostring(value or ""))
end

local function eventKey(event)
    local componentType = text(event, "componentType")
    local interaction = text(event, "interaction")
    if componentType == "" or interaction == "" then
        return ""
    end
    return componentType .. ":" .. interaction
end

local function copySpec(spec)
    local request = {}
    if spec == nil then
        return request
    end
    for k, v in pairs(spec) do
        if k ~= "sourceOn" and k ~= "sourceOff" then
            request[k] = v
        end
    end
    return request
end

local ctx = {
    text = text,
    lower = lower,
    eventKey = eventKey,
    copySpec = copySpec
}

local function componentType(event)
    return text(event, "componentType")
end

local function elementFor(event)
    return elements[componentType(event)]
end

local function soundKey(event)
    local element = elementFor(event)
    if element ~= nil and element.soundKey ~= nil then
        return element.soundKey(event, ctx)
    end
    return eventKey(event)
end

local function edgeEnabled(event)
    local element = elementFor(event)
    if element ~= nil and element.edgeEnabled ~= nil then
        return element.edgeEnabled(event, ctx)
    end
    local interaction = text(event, "interaction")
    if interaction == "hover" then
        return event.bool("value")
    end
    return true
end

local function requestFor(event, spec)
    if spec == nil then
        return nil
    end

    local element = elementFor(event)
    local request
    if element ~= nil and element.request ~= nil then
        request = element.request(event, spec, ctx)
    else
        request = copySpec(spec)
    end

    if request == nil or request.source == nil or request.source == "" then
        return nil
    end
    return request
end

local function dedupe(event, request)
    local interaction = text(event, "interaction")
    if interaction ~= "click" and interaction ~= "submit" and interaction ~= "select" then
        return false
    end
    local clock = os and os.clock
    if clock == nil then
        return false
    end
    local now = clock()
    local key = tostring(request.source or "") .. "|" .. text(event, "nodeId") .. "|" .. text(event, "id")
    if key == lastKey and (now - lastAt) < dedupeSeconds then
        return true
    end
    lastKey = key
    lastAt = now
    return false
end

function M.register(element)
    if element == nil or element.type == nil then
        return
    end
    elements[tostring(element.type)] = element
    if element.audio ~= nil then
        for key, spec in pairs(element.audio) do
            sounds[tostring(key)] = spec
        end
    end
end

local function installDefaultElements()
    M.register(require("engine-ui.lua.ui_elements.button"))
    M.register(require("engine-ui.lua.ui_elements.checkbox"))
    M.register(require("engine-ui.lua.ui_elements.combobox"))
    M.register(require("engine-ui.lua.ui_elements.slider"))
end

function M.disable(key)
    if key ~= nil then
        disabled[tostring(key)] = true
    end
end

function M.enable(key)
    if key ~= nil then
        disabled[tostring(key)] = nil
    end
end

function M.override(key, spec)
    if key ~= nil and spec ~= nil then
        sounds[tostring(key)] = spec
    end
end

function M.install()
    if installed then
        return
    end
    installed = true

    installDefaultElements()

    ui = ui or {}
    ui.defaultAudio = M

    local function handleInteractionAudio(event)
        if not event.bool("enabled") then
            return
        end
        if not edgeEnabled(event) then
            return
        end

        local key = soundKey(event)
        if key == "" or disabled[key] then
            return
        end

        local spec = sounds[key]
        if spec == nil then
            return
        end

        local request = requestFor(event, spec)
        if request == nil then
            return
        end
        if dedupe(event, request) then
            return
        end

        Game.emit("audio.play", request)
    end

    on("ui.component.event", handleInteractionAudio)
end

return M
