# HELIX engine-ui: DOM Style, Metadata Contract and Structure Audit

Статус: рабочий архитектурный документ для ревью engine-ui.
Дата: 2026-06-23.
Область: `modulesSrc/engine-ui`.

## 1. Назначение документа

Этот документ фиксирует единый стиль разработки `engine-ui` после перехода к HTML/CSS/Lua-driven UI runtime.
Он нужен, чтобы остановить расползание логики по большим классам, повторение парсеров/апплаеров и смешение DOM/CSS/runtime/editor/game semantics.

Документ отвечает на четыре вопроса:

1. Где сейчас в `engine-ui` есть риск монолитов.
2. Где логика размазана или повторяется.
3. Каким должен быть единый DOM-принцип для тегов, метаинформации и action/data protocol.
4. Как писать новые HTML-теги, CSS-свойства и runtime behavior без превращения `engine-ui` в набор `if/switch`.

## 2. Базовая формула engine-ui

```text
HTML = structure / templates
CSS  = layout / paint / typography / runtime state
Lua  = behavior / config / bindings / template routing
Java = parser / registry / typed runtime / renderer / diagnostics
```

Это не браузерный DOM.

Запрещено:

```html
<button onclick="save()">Save</button>
```

Правильно:

```html
<button action="boot.logo.save" data-target-id="timeline">Save</button>
```

`HTML` объявляет намерение. `Lua` или native registry определяет, что значит `boot.logo.save`. `Java` только диспатчит typed event.

## 3. Audit summary

Текущий `engine-ui` уже не выглядит одним монолитом: большая часть vocabulary вынесена в registry/definition-пакеты:

- `html/tags` — HTML vocabulary.
- `css/properties` — CSS property specs.
- `markup/internal/factory` — composer dispatch.
- `css/runtime` — runtime style application.
- `node` — retained tree/input/z-order.
- `binding` — data binding descriptors/runtime.
- `command` / `events` — action dispatch.

Но есть зоны риска.

### 3.1 Крупные файлы

| Файл | Lines | Риск | Что не так | Решение |
|---|---:|---|---|---|
| `components/UiSettingsFieldsNode.java` | 848 | High | Один класс держит grouping, tabs, row factory, rendering, scroll, combo overlay, events, i18n warnings. | Split: `SettingsFieldsModel`, `SettingsFieldsLayout`, `SettingsFieldsRenderer`, `SettingsFieldsInputController`, `SettingsRowFactory`. |
| `css/UiCssLayoutEngine.java` | 773 | High | Layout содержит несколько алгоритмов и value resolution в одном месте. | Split by layout strategy: block/flex/absolute/fixed/measure/value-resolver. |
| `markup/internal/parse/UiMarkupXmlParser.java` | 467 | Medium | Parser, recovery, token-level decisions and diagnostics в одном классе. | Split: tokenizer/scanner, tree builder, recovery policy, diagnostics factory. |
| `render/awt/AwtUiRenderContext.java` | 448 | Medium | Backend-specific renderer большой, но приемлем как adapter. | Не трогать typography/text invariant без отдельной задачи. |
| `markup/internal/compile/UiDomStyleBridge.java` | 379 | Medium/High | DOM import, stylesheet load/cache, cascade, state simulation, attribute fallback. | Split: `StyleResourceLoader`, `MarkupDomImporter`, `StateStyleComputer`, `AttributeFallbackApplier`. |
| `markup/internal/factory/UiMarkupInputFactory.java` | 365 | Medium/High | `input`, checkbox, slider, combo-box, option filtering and overlay children mixed. | Split into `InputComposer`, `CheckboxComposer`, `SliderComposer`, `ComboBoxComposer`. |
| `css/runtime/UiCssNodeStyleApplier.java` | 322 | Medium | Box/paint/text/icon/button/checkbox/combo/transform application in one class. | Split into typed appliers: `BoxStyleApplier`, `PaintStyleApplier`, `TextStyleApplier`, `ControlStyleApplier`, `TransformStyleApplier`. |

### 3.2 Пакеты по размеру

Крупнейшие зоны:

| Package | Lines | Вывод |
|---|---:|---|
| `css` | ~5859 | Правильная зона роста, но требует жестких границ между parser/cascade/property/runtime. |
| root `ui` | ~4695 | Содержит старые общие классы; часть стоит постепенно увести в подмодули. |
| `markup` | ~3845 | Нормально, но parser/factory/compile должны оставаться разделенными. |
| `uiComponents` | ~3012 | Компоненты допустимы, но interactive controls не должны копировать input/state logic. |

### 3.3 Повторы / размазанная логика

| Повтор | Где проявляется | Правильная точка владения |
|---|---|---|
| comma-separated CSS list splitting | transition resolver, animation resolver, parser helpers | `UiCssValueLists` |
| time parsing `ms/s/number` | transition resolver, animation resolver | `UiCssTimeParser` |
| color parsing/interpolation | node style applier, transition interpolator | `UiCssColorParser` + `UiCssValueInterpolator` |
| state merging | `UiCssStyleRuntimeController`, DOM state style computation | `UiCssStateStyleResolver` |
| tag fallback / aliases | parser + tag registry + factory | only `UiHtmlTagRegistry` owns fallback semantics |
| interactive control children | button/text/input factories | composer-local policy plus common `UiInteractiveChildPolicy` |
| action/data payload extraction | action binder, editor diagnostics, components | `UiActionPayload` / `UiActionEvent` |

## 4. Layer law: кто чем владеет

| Layer | Owns | Must not own |
|---|---|---|
| `html/tags` | tag name, aliases, composer id, allowed attrs, status, docs, fallback replacement | component construction, rendering, game/editor behavior |
| `css/properties` | property name, aliases, initial/inherited flags, shorthand expansion, validation | node mutation, asset loading, renderer calls |
| `css/cascade` | selector matching, specificity, inheritance, variable resolution | concrete node classes, game/editor action logic |
| `css/runtime` | style transition/animation timelines, typed style application | parsing selectors, compiling DOM, loading CSS files |
| `markup/parse` | tolerant HTML parsing, spans, diagnostics, recovery | node creation, CSS layout, action dispatch |
| `markup/compile` | orchestration from document to retained tree | individual tag semantics, control-specific behavior |
| `markup/factory` | tag spec -> composer -> node dispatch | central `switch(tag)`, game-specific UI decisions |
| `node` | retained tree, bounds, visibility, z-order, traversal, input route | CSS parsing, tag vocabulary, Lua actions |
| `command/events` | typed action event and EventContext | Lua implementation details, visual layout |
| `lua` / game/editor modules | behavior, routing, bindings, templates | rendering nodes directly, mutating engine internals |
| `resources/styles` | module-owned base component styles and user-agent defaults | game/app-specific themes or content-owned policy |

## 5. DOM principle

### 5.1 DOM в HELIX

HELIX DOM — это не browser DOM. Это typed declarative tree:

```text
UiMarkupDocument
  -> UiMarkupNode
  -> UiDomDocument for CSS cascade
  -> retained Node tree for runtime/render/input
```

DOM node не должен знать game semantics.

Допустимые generic concepts:

```text
id, class, style, action, command, data-*, bind-*, title, disabled, checked, value, src
```

Недопустимо в engine-ui core:

```text
player.xp, player.health, enemy.hp, topdown.ui.bindings, assets/topdown/editor_policy.lua
```

### 5.2 DOM metadata contract

Каждый публичный HTML tag должен иметь метаинформацию.

Рекомендуемый contract:

```java
public record UiHtmlTagMeta(
        String name,
        Set<String> aliases,
        String composerId,
        UiHtmlTagCategory category,
        UiHtmlContentModel contentModel,
        Set<String> allowedAttributes,
        boolean voidTag,
        boolean rawText,
        boolean interactive,
        UiHtmlDefinitionStatus status,
        String replacement,
        String usefulAction,
        String description
) { }
```

Минимальные категории:

```text
ROOT
CONTAINER
TEXT
MEDIA
ICON
CONTROL
STYLE_RAW
TEMPLATE
ENGINE_SPECIAL
DEPRECATED
```

Минимальные content model values:

```text
FLOW
TEXT_ONLY
RAW_TEXT
VOID
OPTIONS_ONLY
MIXED_CONTROL
```

### 5.3 Полезное действие тега

`usefulAction` — это не JS handler. Это описание рекомендованного engine action protocol.

| Tag category | Useful action |
|---|---|
| `button` | `action="domain.intent"` + `data-*` payload. |
| `container/card` | selection/open/toggle через `action`, если контейнер интерактивный. |
| `input/checkbox/slider` | `bind-value`, `name`, optional `action` on change. |
| `combo-box/select` | `option` data model + optional visual children, `bind-value`. |
| `img/icon` | usually passive; interactive only with explicit `action`. |
| `style` | no action; raw CSS payload. |
| `template` | no action; template body / future routing. |
| `key-capture` | action/data for captured key events. |

## 6. Canonical tag style

### 6.1 Tag spec style

```java
public final class ButtonHtmlTag implements UiHtmlTagSpec {
    public String name() { return "button"; }
    public Set<String> aliases() { return Set.of(); }
    public String composerId() { return "button"; }
    public UiHtmlDefinitionStatus status() { return STABLE; }
    public String replacement() { return ""; }
    public Set<String> allowedAttributes() {
        return UiHtmlCommonAttributes.interactiveControl();
    }
    public UiHtmlTagMeta meta() { ... }
}
```

Правило: tag spec — только vocabulary/metadata. Никакой сборки node, никакого renderer, никакой Lua-specific логики.

### 6.2 Common attribute sets

Вместо копирования `Set.of(...)` в каждом tag:

```java
UiHtmlCommonAttributes.common();
UiHtmlCommonAttributes.actionable();
UiHtmlCommonAttributes.bindable();
UiHtmlCommonAttributes.media();
UiHtmlCommonAttributes.input();
UiHtmlCommonAttributes.root();
```

Базовый набор:

```text
id, class, style, title
```

Actionable:

```text
action, command, data-action, event, data-*
```

Bindable:

```text
bind, bind-text, bind-value, bind-visible, bind-class, bind-style, bind-x, bind-y, bind-w, bind-h
```

Input/control:

```text
type, name, value, checked, disabled, min, max, step, placeholder
```

Media/icon:

```text
src, alt, width, height, fa, icon, icon-size, icon-scale
```

Root/body:

```text
stylesheet, ui-skins, ui-chrome, chrome, skins, skin-set, skinset
```

## 7. Canonical CSS style

### 7.1 Property specs

Каждое CSS property должно быть definition object:

```java
public final class OpacityCssProperty extends UiCssStringPropertySpec {
    public OpacityCssProperty() {
        super("opacity", Set.of(), true);
    }
}
```

Для shorthand:

```java
public final class TransitionCssProperty extends UiCssStringPropertySpec implements UiCssShorthandPropertySpec {
    public List<UiCssDeclaration> expand(UiCssParseContext context, String rawValue) { ... }
}
```

Правило: shorthand only expands. Он не должен применять runtime style.

### 7.2 Cascade не в property

Cascade owns:

```text
selector match
specificity
order
inline style priority
inheritance
variables
shorthand expansion order
```

Property owns:

```text
name
aliases
attributeFallback
value validation
shorthand expansion if needed
```

Runtime applier owns:

```text
Node mutation
bounds/paint/text/control state transform
```

### 7.3 Base component styles

Каждый публичный UI component/tag обязан иметь базовый стиль в module-owned resources:

```text
modulesSrc/engine-ui/src/main/resources/engine-ui/styles/
```

Правило владения:

```text
engine-ui resources = минимальные defaults компонента
application/game/editor stylesheet = overrides конкретного продукта
```

Базовые стили загружаются через `engine-ui/styles/user-agent.styles` и применяются как user-agent layer. Поэтому application stylesheet из `stylesheet="..."` и inline `<style>` должны побеждать defaults по cascade priority.

Нельзя переносить эти defaults в `assets/<game>/...`: это module-owned component policy, а не game-only content.

## 8. Anti-monolith rules

### 8.1 Размер файлов

| File size | Правило |
|---:|---|
| `< 200` lines | нормальный target |
| `200-300` lines | допустимо для сложного adapter/parser fragment |
| `300-500` lines | нужен split plan |
| `> 500` lines | архитектурный debt, запрет на добавление новых responsibilities |

Исключения: generated files, dense tables, deliberately isolated parser backend with tests.

### 8.2 Запрещено добавлять новую ответственность в крупные файлы

Если файл уже >300 lines, новые функции добавляются только через extraction.

Пример:

```text
UiSettingsFieldsNode > 800 lines
```

Нельзя добавлять туда новый settings widget напрямую. Сначала выделить row factory/renderer/input controller.

### 8.3 No central switch by vocabulary

Запрещено:

```java
switch (markup.tag()) { ... }
if ("display".equals(property)) { ... }
```

Допустимо:

```java
switch (style.layout().display().kind()) { ... }
```

Разница: запрещён string vocabulary switch; разрешён switch по typed engine model.

## 9. Recommended split plan

### Pass A — non-invasive documentation + guards

- Keep this document.
- Add line-count guard for engine-ui files over 500 lines.
- Add no-domain scan for `topdown`, `player.xp`, `onclick`, `document.querySelector`.
- Add allowlist for `CrashWindow*` false positives if scanning `window.`.

### Pass B — tag metadata extraction

- Add `UiHtmlTagMeta`.
- Add `UiHtmlTagCategory`.
- Add `UiHtmlContentModel`.
- Add `UiHtmlCommonAttributes`.
- Gradually migrate tag specs.

### Pass C — split monolith candidates

1. Split `UiSettingsFieldsNode`.
2. Split `UiCssNodeStyleApplier`.
3. Split `UiMarkupInputFactory`.
4. Split `UiDomStyleBridge`.
5. Split `UiCssLayoutEngine` into strategy classes.

### Pass D — shared CSS utility extraction

- `UiCssValueLists` for comma lists.
- `UiCssTimeParser` for ms/s/negative time.
- `UiCssColorParser` for color parse.
- `UiCssValueInterpolator` for numeric/color/transform interpolation.

## 10. Merge checklist

Before merging engine-ui changes:

- [ ] No new file over 300 lines without split justification.
- [ ] No added `switch(tagName)` or `if ("tag"...)` in factory/compile layers.
- [ ] No `onclick` or JS-like event handler strings.
- [ ] Behavior routed through `action` / `command` / `data-*`.
- [ ] New tag has `UiHtmlTagMeta` or a migration note.
- [ ] New CSS property has property spec or explicit documented pass-through consumer.
- [ ] New UI component/tag has base CSS under `src/main/resources/engine-ui/styles` or a documented reason why it is raw/invisible.
- [ ] New shorthand only expands declarations.
- [ ] Runtime applier does not parse stylesheet or selectors.
- [ ] Parser does not compose runtime nodes.
- [ ] Engine-ui does not reference TopDown/game-only paths.
- [ ] Text rendering code untouched unless task explicitly targets typography.

## 11. Acceptance criteria

`engine-ui` считается clean, если:

1. HTML vocabulary lives in tag definitions and metadata.
2. CSS vocabulary lives in property specs and shorthand definitions.
3. Behavior lives in Lua/action registry, not inline JS-like attributes.
4. Parser errors become diagnostics in runtime/editor modes.
5. Strict mode can fail templates in tests.
6. Factories dispatch by registry, not by central tag switch.
7. CSS runtime applies typed style state, not arbitrary raw strings everywhere.
8. Large UI components are split into model/layout/render/input/factory.
9. Base component styles live in `engine-ui` module resources and are overridable by application styles.
10. No game-specific nouns or TopDown-specific content in engine-ui runtime.
11. Documentation and tests describe vocabulary, migration paths and known limitations.

## 12. Final rule

```text
Definitions own vocabulary.
Registries resolve meaning.
Runtime executes typed protocols.
Lua owns behavior.
Java owns generic machinery.
```

Shorter:

```text
HTML declares.
CSS computes.
Lua decides.
Java executes.
```


## 13. Implementation status

Implemented in the current pass:

- `UiHtmlTagMeta` public metadata contract.
- `UiHtmlTagCategory` and `UiHtmlContentModel`.
- `UiHtmlCommonAttributes` for shared attribute groups.
- `UiHtmlTagSpec.meta()` default metadata access.
- Migration of high-traffic tags (`body`, `button`, `input`, `img`, `i/icon`, `checkbox`, `combo-box`, `slider`, `style`, `key-capture`, `settings-fields`) away from raw repeated `Set.of(...)` attribute lists.
- Metadata tests for tag category/content model/useful action.
- Browser-script guard for `onclick`, `document.querySelector` and `eval(` in engine-ui main code/resources.
- Monolith guard: no new engine-ui Java file over 500 lines unless explicitly allowlisted as known split debt.

Known split debt remains:

- `UiSettingsFieldsNode.java`
- `UiCssLayoutEngine.java`
- `UiMarkupXmlParser.java`
- `AwtUiRenderContext.java`
- `UiDomStyleBridge.java`
- `UiMarkupInputFactory.java`
- `UiCssNodeStyleApplier.java`

`UiSettingsFieldsNode` split pass started and implemented:

- `UiSettingsFieldsGroup` and `UiSettingsFieldsRow` records moved to `components/settingsfields`.
- `UiSettingsFieldsModel` now owns descriptor grouping, active-group normalization, group titles, group icons and group ordering.
- `UiSettingsRowFactory` now owns setting-control construction and settings interaction dispatch.
- `UiSettingsFieldsMath` owns small math/geometry helpers.
- `UiSettingsFieldsLayout` owns row-control width policy.
- `UiSettingsFieldsNode` is reduced below 500 lines and now acts as the retained owner/composition node for render/input state.

Renderer, geometry and input orchestration are now extracted as `UiSettingsFieldsRenderer`, `UiSettingsFieldsGeometry` and `UiSettingsFieldsInputController`. `UiSettingsFieldsNode` is now about 300 lines and primarily owns lifecycle, retained child composition and state handoff to helpers. Remaining for a later pass: move the remaining row relayout loop into a dedicated layout coordinator if the target is below 250 lines.


### 13.1 Current split continuation

Implemented after the renderer/geometry/input split:

- `UiSettingsFieldsRelayoutCoordinator` now owns the remaining row relayout loop: rows clip bounds, row y positions, label/control width calculation, row node bounds and overflow-warning decision.
- `UiSettingsFieldsNode` delegates relayout and remains focused on lifecycle, state, group rebuild, row attachment and snapshot creation.
- Base component CSS resources are now module-owned under `src/main/resources/engine-ui/styles` and loaded through `engine-ui/styles/user-agent.styles`.
- Checkbox/combo visual defaults were removed from Java composition fallback code and moved into overridable base CSS resources.
