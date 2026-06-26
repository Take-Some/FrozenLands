package org.takesome.frozenlands.engine.runtime;

import org.takesome.frozenlands.engine.EngineContext;

/**
 * Installs one runtime graph node into the FrozenLands host.
 *
 * <p>Implementations are discovered through {@link java.util.ServiceLoader} from
 * independent Gradle module jars. Core owns the host/SPI only; runtime modules
 * own concrete services, providers and EngineModule adapters.</p>
 */
public interface EngineRuntimeInstaller {
    default int priority() {
        return 0;
    }

    String id();

    void install(EngineContext context);
}
