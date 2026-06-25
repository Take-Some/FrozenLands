# `dev.takesome.helix.ui.scene`

## Responsibility

UI scene and scene manager model.

## Intended Usage

Use this package to manage active UI scenes and scene transitions.

Prefer importing the narrowest public contract available. Do not bypass a facade to reach implementation details unless this README explicitly marks the package as an implementation package.

## Public Surface

- class `NodeScene` (NodeScene.java)
- interface `Scene` (Scene.java)
- class `SceneManager` (SceneManager.java)

## API Boundary

Subsystem-owned public classes. They may be used by nearby engine/game composition code, but avoid treating every class here as a global API.

## Deprecation Status

**Status:** Not deprecated.

Keep active unless superseded by a package-specific migration note in this file or in `package-info.java`.

## Future Direction

Continue consolidating UI scene graph, layout, animation, and rendering boundaries without leaking concrete renderer details into game logic.

## Maintenance Rules

- Keep one package-level responsibility. If a class starts serving another subsystem, move it to a clearer package.
- Do not introduce circular dependencies between sibling packages.
- Prefer immutable model/value objects for data transfer across subsystem boundaries.
- Keep framework-specific code in adapter packages such as `gdx`, `io`, or other explicitly named integration packages.
- Update this README when adding a new public class, changing deprecation status, or moving responsibilities.

## Source Files

- `NodeScene.java`
- `Scene.java`
- `SceneManager.java`
