-- FrozenLands startup script.
-- Executes through engine.core.script.run using the Java-backed Lua core bridge.

local core = require("engine.core")

core.emit("core.script.loaded", {
  script = script.name,
  path = script.path,
  autorun = args.autoRun == true
})

return true
