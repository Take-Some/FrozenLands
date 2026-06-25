package org.takesome.frozenlands.engine.config;

@Deprecated
public abstract class ConfigAbstract {
    protected ConfigAbstract() {
        throw new UnsupportedOperationException("Legacy config bridge was removed. Use ConfigReader.");
    }
}
