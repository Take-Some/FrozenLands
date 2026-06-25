package org.takesome.frozenlands.engine.core.console;

import java.util.Map;

record ConsoleRequest(String command, String rawArguments, Map<String, Object> arguments) {
}
