# FrozenLands HTML UI

`assets/ui` is the game UI authoring root. New UI screens are written as HTML-like documents, styled with CSS, and scripted through Lua hooks exposed by HtmlDom.

## Layout

```text
assets/ui/
├── ui.manifest.json          # single source of truth for UI documents/styles/scripts
├── app/                      # document entrypoints
├── system/                   # reset, tokens and global layout primitives
├── themes/                   # project visual identity
├── components/               # reusable component CSS
├── screens/                  # screen-specific CSS
├── scripts/                  # Lua UI bridge scripts
├── data/                     # declarative bindings/action maps
├── docs/                     # authoring notes
├── forms/                    # legacy JSON forms kept for compatibility
└── icons/                    # legacy/runtime icons
```

## Rules

1. Java runtime code loads UI through `ui.manifest.json`; it must not hardcode screen file paths.
2. HTML owns structure, CSS owns presentation, Lua owns UI actions and DOM mutations.
3. `forms/` remains legacy. New UI goes into `app/`, `components/`, `screens/`, `system/`, `themes/`, `scripts/` and `data/`.
4. Use `data-action` for game actions. Do not encode game logic into CSS classes.
5. Runtime state is represented by `data-state`, `data-mode`, `data-visible` and pseudo-state, not by ad-hoc Java painting branches.

## First screens

- `app/frozenlands.html` — root shell.
- `app/hud.html` — in-game HUD fragment.
- `app/pause.html` — pause/system menu.
- `app/main-menu.html` — first main menu draft.
