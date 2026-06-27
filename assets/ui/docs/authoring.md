# UI authoring notes

## Screen lifecycle

The runtime should mount exactly one root document from `ui.manifest.json.entry`. Individual screens are normal DOM sections inside the shell and are shown/hidden by runtime state.

Recommended shell states:

```text
data-screen="hud"
data-screen="pause"
data-screen="main-menu"
data-mode="gameplay|paused|boot"
```

## Action contract

Use `data-action` on interactive elements:

```html
<button data-action="game.resume">Resume</button>
```

The Lua hook receives the action and element id. Java may still perform native handling after Lua unless Lua returns `true` from `onClick`.

## Styling contract

- `system/tokens.css` defines colors, spacing, typography, z-index and transition tokens.
- `themes/frozenlands.css` defines project mood.
- `components/*.css` defines reusable components.
- `screens/*.css` defines screen-specific composition only.
