local M = { id = "engine.bootstrap", commands = {} }

function M.call(command, args)
  args = args or {}
  return java.callModule(M.id, command, args)
end

return M
