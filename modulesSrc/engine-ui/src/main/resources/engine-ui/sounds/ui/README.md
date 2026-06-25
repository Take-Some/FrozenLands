# HELIX engine-ui sounds

Module-owned UI sound assets. Selection and playback are script-owned.

## Layout

```text
button/
  hover.ogg
  press.ogg
  submit.ogg
checkbox/
  on.ogg
  off.ogg
  toggle.ogg
combobox/
  open.ogg
  close.ogg
  select.ogg
  open_alt.ogg
  close_alt.ogg
slider/
  commit.ogg
```

## Runtime ownership

Java only emits generic retained UI component events.
Java must not choose concrete UI sound files and must not hardcode button semantics.

Default mapping lives in:

```text
engine-ui/lua/ui_default_interaction_audio.lua
```

Current default script behavior:

- `button:hover=true` -> `button/hover.ogg`
- normal `button:click` -> `button/press.ogg`
- `button type="submit"` click -> `button/submit.ogg`
- `button type="reset"` click -> `button/reset.ogg`
- `button:press` event is ignored by the default audio script to avoid press+click double playback
- combo-box open/close/select -> `combobox/open.ogg`, `combobox/close.ogg`, `combobox/select.ogg`
- checkbox changed -> `checkbox/on.ogg` or `checkbox/off.ogg`
- slider commit -> `slider/commit.ogg`

Game/editor scripts may override entries through `ui.defaultAudio.override(key, spec)` after the default script is installed.
