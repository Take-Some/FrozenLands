# FrozenLands 1.4.2

## Summary

FrozenLands 1.4.2 integrates the SkySimulation command path with the engine-owned jME frame loop. NOESIS and Lua now submit sky intent while Java/SkyControl owns transition state and jMonkeyEngine owns frame timing.

## Runtime changes

- Added `engine.sky` Lua/API module descriptor and facade.
- Added sky command bridge for canonical SkySimulation command ids.
- Added render/update-thread command queue before `SkyControl` update.
- Kept atmosphere transitions passive: commands start transitions, `SkyControl.controlUpdate(tpf)` advances them.
- Added headless/test `Sky.tick(deltaSeconds)` facade for non-jME runtime hosts only.
- Added runtime manifest diagnostics for `engine.sky`.

## Architecture contract

```text
NOESIS / Lua
    chooses intent and sends sky commands

Java Sky runtime
    owns sky state, active transitions, command execution, materials

jMonkeyEngine
    owns frame loop and supplies tpf through controlUpdate(tpf)
```

## Verified

- `gradlew.bat build`
- `gradlew.bat :core:compileJava`

