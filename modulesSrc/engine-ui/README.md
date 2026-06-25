# Helix Engine UI Module

`engine-ui` is the engine-side UI capability for Helix/HELIX. It provides generic UI documents, retained scene nodes, widgets, painting primitives, icon fonts, markup parsing, markup compilation, and capability registration. The module is intentionally engine-generic: it must not encode game concepts such as player XP, health, enemies, quests, inventory, or biome-specific UI.

## Architectural rule

The UI module follows the same boundary as the rest of the engine:

- Game or Lua data declares layouts, bindings, state paths, assets, and game-specific meaning.
- Java loads, validates, compiles, renders, and updates generic descriptions.
- Engine UI code may know infrastructure keys such as `id`, `target`, `source`, `type`, `expr`, `format`, `visible`, `enabled`, `text`, and `progress`.
- Engine UI code must not know game-domain paths such as `player.xp`, `player.health`, `enemy.hp`, `inventory.gold`, or quest-specific state.

This keeps `engine-ui` reusable for top-down, platformer, editor, simulation, menu, and tooling screens without rewriting Java for each game definition.

## Main responsibilities

`engine-ui` owns these engine capabilities:

- UI value objects: colors, rectangles, layout descriptors, theme descriptors, skins, widget descriptors, and document definitions.
- UI scene graph integration: retained UI nodes, container nodes, component nodes, scene loading, and scene-level root composition.
- Rendering contracts and adapters: `UiPainter`, document renderers, widget renderers, progress bar renderer, text renderer, texture region resolution, and GDX painter integration.
- Markup pipeline: public parser/compiler facades plus internal XML parsing, CSS-like style resolution, node factory dispatch, layout application, root decoration, module gates, and menu contribution loading.
- Icon system: generic icon contracts, Font Awesome style descriptors, font resources, GDX font family creation, and icon registries.
- Runtime affordances: input routing helpers, animation support, capability registration, and no-op degraded providers.

## Public package map

### `dev.takesome.helix.ui`

Core UI descriptors, rendering contracts, layout values, theme definitions, painter abstractions, and generic renderer utilities.

Important public roles:

- `UiDocument`, `UiTheme`, `UiLayout`, `UiWidgetDefinition`, `UiPanelDefinition`, `UiElementSkin`
- `UiPainter`, `UiDocumentRenderer`, `UiWidgetRenderers`, `UiProgressBarRenderer`, `UiTextRenderer`
- `UiColor`, `UiRect`, `UiAnchor`, `TextAlign`, `UiBindingSource`
- GDX adapter classes such as `GdxUiPainter` and `GdxUiElementRenderer`

### Library entry point

Use `EngineUi` as the public facade for game, editor and standalone consumers. It centralizes renderer wiring and keeps callers away from low-level renderer internals such as `GdxUiPainter`, `NineSliceRenderer`, `TextureRegionMetrics`, `UiAnimationPipeline`, and render passes.

```java
EngineUiRuntime ui = EngineUi.create(assets, materials);
UiDocument document = ui.loadFirstWithPanels("ui/game.ui.json", "game/ui/game.ui.json");

ui.setI18n(i18n);
ui.setDrawTracers(debug);
ui.renderFrame(document, bindings, batch, assets.font(), width, height);
```

For custom standalone tools, use the config builder:

```java
EngineUiRuntime ui = EngineUi.create(
        EngineUi.configure(assets)
                .materials(materials)
                .defaultFont(font)
                .i18n(i18n)
                .build()
);
```

`UiDocumentRenderer` remains public for advanced integration, but normal consumers should enter through `EngineUi` / `EngineUiRuntime`.

### `dev.takesome.helix.ui.node`

Retained UI node primitives and scene-node infrastructure. Use these classes to build and compose UI trees without coupling them to a specific renderer.

### `dev.takesome.helix.ui.components`

Concrete retained widgets such as buttons, labels, inputs, checkboxes, panels, and related component nodes. These are still generic UI widgets, not game-specific HUD classes.

### `dev.takesome.helix.ui.markup`

Public HELIX UI markup API. The root package is deliberately small and facade-oriented:

- `UiMarkupNode` immutable parsed element DTO
- `UiMarkupDocument` immutable document root wrapper
- `UiMarkupParser` public parser facade
- `UiMarkupCompiler` public compiler facade
- `UiMarkupProvider` provider contract
- `UiMarkupSceneLoader` scene-root loader facade

Implementation details are in `dev.takesome.helix.ui.markup.internal.*`.

### `dev.takesome.helix.ui.markup.provider`

Public provider implementations:

- `DefaultUiMarkupProvider` loads, parses, and compiles markup documents.
- `NoOpUiMarkupProvider` is the degraded fallback when markup support is unavailable.

### `dev.takesome.helix.ui.markup.internal.*`

Internal markup implementation hierarchy:

- `internal.parse` — XML-compatible markup parsing implementation.
- `internal.compile` — compilation pipeline orchestration.
- `internal.model` — helper accessors for public model DTOs.
- `internal.style` — typed style readers, CSS-like resolver, button skin/icon resolver.
- `internal.layout` — generic layout and state application.
- `internal.factory` — tag-to-node factory dispatch and concrete node factories.
- `internal.root` — root background, overlay, and image decoration.
- `internal.module` — optional module availability gates.
- `internal.menu` — module-provided menu contribution loading.
- `internal.action` — generic event/action binding from markup style maps.

### `dev.takesome.helix.ui.icons`

Generic icon contracts, registries, Font Awesome descriptors, font resources, and GDX font factory adapters. Icon style metadata belongs to descriptors, not to game-specific UI code.

### Toast capability boundary

Transient notifications are owned by the standalone `engine-toast` module / `engine.toast` capability. `engine-ui` only provides the generic retained UI, markup, style, input and render primitives consumed by that capability.

### `dev.takesome.helix.ui.capability`

Capability registration for feature providers and markup providers. This package connects UI services to the engine capability registry and supplies degraded fallbacks.

### `dev.takesome.helix.ui.animation`

Generic UI animation registry and text animation pipeline.

- `UiAnimationRegistry` stores available animation effects and aliases.
- `UiAnimationDescriptor` registers one concrete effect instance plus its aliases.
- `UiAnimationManifest` declares animation packs in data/resource form.
- `UiAnimationManifestValidator` validates namespaces, ids, aliases, implementation classes, and constructors before registry mutation.
- `UiAnimationDescriptorLoader` loads manifests from classpath resources or engine data paths, validates them, and converts entries into registry descriptors.
- `UiAnimationPipeline` composes registered text effects for a render pass.
- Built-in effect classes live in `dev.takesome.helix.ui.animation.effects`: `TypewriterTextAnimation`, `FadeTextAnimation`, and `SlideTextAnimation`.
- The built-in pack is declared in `dev/takesome/helix/ui/animation/builtin-text-animations.json`.
- `UiDocument.animationManifests`, `UiDocument.animationPacks`, `UiDocument.theme.animationManifests`, and `UiDocument.theme.animationPacks` declare document/theme-local animation packs.
- `UiAnimationPipeline.configure(document)` reloads manifest namespaces only when the document/theme animation declaration signature changes.

To add a built-in animation, create a new `UiTextAnimationEffect` class under `animation.effects`, add it to an animation manifest, validate/load it through `UiAnimationDescriptorLoader`, and install or reload its namespace in `UiAnimationRegistry`. Game-specific animation selections and custom packs should come from descriptors/data, not from Java branches.

### `dev.takesome.helix.ui.input`, `layout`, `render`, `scene`, `workspace`

Focused support packages for input, layout, rendering, scene integration, and editor/workspace-oriented UI helpers.




## Document-declared widget primitive packs

Widget drawing primitives are registry-driven. Built-in primitives are declared by resource manifest:

```text
dev/takesome/helix/ui/primitives/builtin-widget-primitives.json
```

UI documents and themes may add aliases, enable packs, disable packs, or reload primitive namespaces:

```json
{
  "primitivePacks": [
    {
      "id": "custom.widget.primitives",
      "namespace": "custom.widget.primitives",
      "primitives": [
        { "id": "headline", "renderer": "text", "aliases": ["headline-text"] },
        { "id": "meter", "renderer": "bar", "aliases": ["meter-bar"] }
      ]
    }
  ]
}
```

`UiWidgetPrimitiveRegistryLoader` validates manifest entries against already registered renderer ids, then reloads the declared namespace. `enabled: false` unregisters the namespace. `UiWidgetRenderers` also has generic fallback primitive resolution, so semantic widget `type` values can still draw when `primitive` is omitted: text fields fall back to `text`, progress/base/fill fields to `bar`, icon fields to `icon`, material fields to `image`, and fillColor fields to `fill`.


## Visibility and render diagnostics

`UiVisibilityDiagnostics` evaluates panel/widget visibility into a structured decision: target, descriptor id, source key, expression, fallback key, default value, final result and reason.

`UiRenderDiagnostics` logs hidden panels/widgets once per unique reason and warns when a widget resolves to an unregistered primitive renderer. This prevents UI from disappearing silently while keeping frame logs bounded.

## Document rendering pipeline

`UiDocumentRenderer` is now an orchestration facade over explicit render passes:

```text
UiDocumentRenderer
  -> UiBindingPreparePass
  -> UiPanelLayoutPass
     -> UiPanelMeasurePass
     -> UiWidgetArrangePass
     -> UiStackLayoutPass
  -> UiPanelRenderPass
  -> UiWidgetRenderPass
     -> UiWidgetDispatchPass
     -> UiWidgetTracePass
     -> UiWidgetPrimitiveRendererRegistry
```

Responsibilities:

- `UiBindingPreparePass` reloads document/theme binding manifests and returns a runtime binding source.
- `UiPanelLayoutPass` coordinates panel layout subpasses and returns ordered render entries.
- `UiPanelMeasurePass` resolves panel rectangles and auto-layout measured size.
- `UiWidgetArrangePass` writes resolved widget bounds for auto-layout panels.
- `UiStackLayoutPass` applies stack group placement after individual panel rectangles are resolved.
- `UiPanelRenderPass` draws panel fills/backgrounds and delegates widgets.
- `UiWidgetRenderPass` coordinates widget dispatch and tracing.
- `UiWidgetDispatchPass` iterates visible widgets and dispatches them to data-selected primitive renderers.
- `UiWidgetTracePass` draws optional panel/widget debug tracing bounds.
- `UiWidgetPrimitiveRendererRegistry` maps primitive ids such as `text`, `bar`, `image`, `stretch`, `fill`, and aliases such as `rect` to renderer implementations.

The renderer should stay a facade. New layout, binding, panel, or widget behavior should be added to the relevant pass or a new pass under `dev.takesome.helix.ui.pipeline`, not directly into `UiDocumentRenderer`. New widget drawing primitives should be registered in `UiWidgetPrimitiveRendererRegistry` instead of adding `if/else` dispatch branches.

## Document-declared binding packs

UI documents and themes may declare binding packs in the same descriptor flow as layouts and animations:

```json
{
  "bindingPacks": [
    {
      "id": "hud.bindings",
      "namespace": "hud.bindings",
      "bindings": [
        {
          "id": "hud.health.text",
          "target": "healthText.text",
          "source": "player.health",
          "type": "number",
          "format": "HP {value}"
        }
      ]
    }
  ]
}
```

`UiBindingManifestValidator` validates ids, targets, source/expression rules, expression syntax and duplicate targets before registry mutation. `UiBindingExpressionEvaluator` provides a safe numeric/boolean mini-DSL for `expr`, including arithmetic, comparisons, `&&`, `||`, `!`, and functions such as `max`, `min`, and `clamp`. `UiBindingRegistry` owns namespace reload/uninstall semantics. `UiDocumentRenderer` creates a runtime binding source from `UiDocument.bindingManifests`, `UiDocument.bindingPacks`, `UiThemeDefinition.bindingManifests`, and `UiThemeDefinition.bindingPacks` before each render pass. Java still treats `source` and `expr` as generic data paths; game meaning remains outside the engine UI runtime.

## Document-declared animation packs

UI documents and themes may declare animation packs without renderer code manually invoking the loader:

```json
{
  "theme": {
    "animationManifests": ["resource:dev/takesome/helix/ui/animation/builtin-text-animations.json"]
  },
  "animationPacks": [
    {
      "id": "custom.ui.animations",
      "namespace": "custom.ui.animations",
      "animations": [
        {
          "id": "pulse",
          "className": "dev.example.PulseTextAnimation",
          "aliases": ["soft-pulse"]
        }
      ]
    }
  ]
}
```

`UiDocumentRenderer` calls `UiAnimationPipeline.configure(document)` before a render pass. The pipeline validates and reloads changed namespaces, while preserving unrelated namespaces.

## Markup pipeline

Runtime flow:

```text
source markup
  -> UiMarkupParser
  -> internal.parse.UiMarkupXmlParser
  -> UiMarkupDocument / UiMarkupNode DTOs
  -> UiMarkupCompiler
  -> internal.compile.UiDomCompilationPipeline
  -> internal.style.UiMarkupCssResolver
  -> internal.module.UiDomModuleGate
  -> internal.root.UiDomRootDecorator
  -> internal.factory.UiDomRetainedNodeFactory
  -> retained UI Node tree
```

`UiMarkupNode` is intentionally a pure immutable DTO. Attribute convenience logic belongs to `internal.model.UiDomAttributes`, keeping the public model small and stable.

## Data-driven UI boundary

The UI runtime should be able to render and update any screen from descriptors. Game-specific layout node names, state paths, derived values, and binding formulas should be declared by data/Lua, not hardcoded in Java.

Allowed engine-level terms include generic protocol and property names:

- `id`, `target`, `source`, `type`, `expr`, `format`, `default`, `update`
- `text`, `visible`, `enabled`, `progress`, `value`, `x`, `y`, `width`, `height`, `color`, `image`, `font`
- component kinds such as `panel`, `button`, `text`, `image`, `input`, `checkbox`, `container`, `list`, and `grid`

Forbidden in engine UI source outside tests/docs:

- `player.xp`, `player.health`, `enemy.hp`, `inventory.gold`, `quest.completed`
- Java methods such as `bindPlayerXp`, `calculateXpRatio`, or game-specific HUD update functions
- concrete game nouns as runtime logic, for example `Goblin`, `HealthPotion`, `ForestBiome`, or `QuestReward`

## Extension guidelines

When adding a UI feature:

1. Decide whether it is a generic engine capability or game definition.
2. Keep generic mechanisms in `engine-ui`.
3. Keep game-specific content in data, Lua, or a game module.
4. Add descriptors and validation before adding runtime behavior.
5. Prefer immutable DTOs at API boundaries.
6. Put parser/compiler/layout internals under `markup.internal.*` instead of expanding root APIs.
7. Keep renderer-specific code in adapter packages.
8. Update this README and related `package-info.java` files when adding packages or changing package responsibilities.

## Build and validation

Useful validation commands:

```bash
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :engine-ui:compileJava --no-daemon
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :engine-ui:test :engine-ui:jar --no-daemon
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :engine-ui:installModule --no-daemon
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain cleanInstalledModules --no-daemon
```

The runtime module install policy is one stable jar per runtime module: `modules/engine-ui.jar`.
