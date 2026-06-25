# Engine UI Patch Notes

## 2026-06-24 — DevTools library pass

### Changed
- Rebuilt `engine-ui/devtools/ui/devtools.css` as a readable DevTools component library: shell, titlebar, command buttons, tabs, panes, cards, DOM tree, property rows, computed tables, badges, code view, box model, preview, diagnostics, and status bar.
- Reworked `engine-ui/devtools/ui/devtools.html` to use the component library with a wider 1280x720-friendly layout and larger readable text sizes.
- Added `engine-ui/devtools/ui/devtools.library.html` as a component showcase / library reference page.
- Added `HtmlDevToolsResources.LIBRARY_MARKUP_RESOURCE` for the built-in library resource contract.

### Guardrails
- Added DevTools UI resources to the authoring conformance test scan so DevTools HTML/CSS stays parser-recoverable.
- Did not modify font loading, text rasterization, antialiasing, hinting, line metrics, or crash typography.

### Verified
- `gradlew.bat --no-daemon --max-workers=1 :engine-ui:test`
- `gradlew.bat --no-daemon --max-workers=1 :engine-scripting:compileJava :gameTypes:topdown:compileJava :desktop:compileJava`
- `gradlew.bat --no-daemon --max-workers=1 :desktop:runTopDownMenuModalSmoke`

## 2026-06-24 — DOM runtime naming pass

### Changed
- Renamed DOM runtime compile/style/layout helpers from historical `UiMarkup*` names to DOM-specific names:
  - `UiMarkupCompilationPipeline -> UiDomCompilationPipeline`
  - `UiMarkupDomStyleBridge -> UiDomStyleBridge`
  - `UiMarkupComputedStyles -> UiDomComputedStyles`
  - `UiMarkupResourcePathResolver -> UiDomResourcePathResolver`
  - `UiMarkupStyleReader -> UiDomStyleReader`
  - `UiMarkupLayoutResolver -> UiDomLayoutResolver`
  - `UiMarkupButtonSkinResolver -> UiDomButtonSkinResolver`
  - `UiMarkupSkinResolver -> UiDomSkinResolver`
  - `UiMarkupRootDecorator -> UiDomRootDecorator`

### Guardrails
- Expanded the DOM runtime legacy guard to reject the old compile/style/layout/root helper symbols in DOM runtime internals.
- Kept `UiMarkupCompiler`, `UiMarkupParser`, `UiMarkupDocument`, and `UiMarkupNode` names as public authoring/compatibility boundary names.

### Verified
- `gradlew.bat --no-daemon --max-workers=1 :engine-ui:test`
- `gradlew.bat --no-daemon --max-workers=1 :engine-scripting:compileJava :gameTypes:topdown:compileJava :desktop:compileJava`
- `gradlew.bat --no-daemon --max-workers=1 :desktop:runTopDownMenuModalSmoke`

## 2026-06-24 — DOM runtime cleanup pass

### Added
- Added an architecture guard that fails if DOM runtime internals reintroduce `UiMarkupNode` / old markup-node factory dependencies.
- Expanded the legacy guard to reject old DOM-helper symbols (`UiMarkupAttributes`, `UiMarkupI18nText`, `UiMarkupModuleGate`) in DOM runtime internals.

### Changed
- Renamed the retained-node compiler from `UiMarkupNodeFactory` to `UiDomRetainedNodeFactory`.
- Renamed DOM-only helpers from legacy `UiMarkup*` names: `UiMarkupAttributes -> UiDomAttributes`, `UiMarkupI18nText -> UiDomI18nText`, `UiMarkupModuleGate -> UiDomModuleGate`.
- Kept `UiMarkupDocument` as a DOM-only wrapper: parser output is `UiDomDocument`; legacy markup is generated lazily only on explicit legacy access.
- Moved stylesheet discovery to DOM traversal: `<style>`, `<link rel="stylesheet">`, and `stylesheet="..."` now read from `UiDomDocument` / `UiDomElement`.
- Moved skin/chrome discovery to DOM traversal and DOM attributes.
- Simplified retained-node composition to the canonical path: `UiDomElement -> style -> composer -> retained Node`.

### Removed
- Removed DOM-to-legacy-markup indexing from `UiMarkupDomStyleBridge`.
- Removed DOM-to-legacy-markup indexing from the retained-node factory path.
- Removed legacy `UiMarkupNode` overloads from internal retained-node factories, module gate, i18n text helpers, and attribute helpers.
- Removed legacy markup import/conversion helpers from `UiDomDocument`; legacy conversion now stays behind `UiMarkupDocument` only.
- Converted optional menu contribution loading from `UiMarkupNode` to `UiDomElement`.
- Removed the old `UiMarkupNodeFactory` symbol from engine-ui source and tests.

### Verified
- `gradlew.bat :engine-ui:compileJava`
- `gradlew.bat :engine-ui:test`
- `gradlew.bat :engine-scripting:compileJava :gameTypes:topdown:compileJava :desktop:compileJava`
- `gradlew.bat :desktop:runTopDownMenuModalSmoke`
