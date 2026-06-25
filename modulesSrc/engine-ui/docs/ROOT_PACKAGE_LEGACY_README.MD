# `dev.takesome.helix.ui`

## Responsibility

Core UI document, rendering, style, painter, and widget infrastructure.

## Intended Usage

Use this package for UI documents and immediate-mode/gdx rendering integration.

Prefer importing the narrowest public contract available. Do not bypass a facade to reach implementation details unless this README explicitly marks the package as an implementation package.

## Public Surface

- class `GdxUiElementRenderer` (GdxUiElementRenderer.java)
- class `GdxUiPainter` (GdxUiPainter.java)
- class `NineSlice` (NineSlice.java)
- class `NineSliceRenderer` (NineSliceRenderer.java)
- interface `RegionMetrics` (RegionMetrics.java)
- enum `TextAlign` (TextAlign.java)
- class `TextureRegionMetrics` (TextureRegionMetrics.java)
- enum `UiAnchor` (UiAnchor.java)
- interface `UiBindingSource` (UiBindingSource.java)
- class `UiColor` (UiColor.java)
- class `UiDefinitionLoader` (UiDefinitionLoader.java)
- class `UiDocument` (UiDocument.java)
- class `UiDocumentRenderer` (UiDocumentRenderer.java)
- class `UiElementSkin` (UiElementSkin.java)
- enum `Kind` (UiElementSkin.java)
- class `UiFontDefinition` (UiFontDefinition.java)
- class `UiJson` (UiJson.java)
- class `UiLayout` (UiLayout.java)
- class `UiLayoutResolver` (UiLayoutResolver.java)
- class `UiNineSliceDefinition` (UiNineSliceDefinition.java)
- interface `UiPainter` (UiPainter.java)
- class `UiPanelDefinition` (UiPanelDefinition.java)
- class `UiPanelLayout` (UiPanelLayout.java)
- class `UiRect` (UiRect.java)
- class `UiRegionDefinition` (UiRegionDefinition.java)
- class `UiTheme` (UiTheme.java)
- class `UiThemeDefinition` (UiThemeDefinition.java)
- class `UiWidgetDefinition` (UiWidgetDefinition.java)

## Package-Private / Internal Surface

- class `AlphaGridNineSlice` (AlphaGridNineSlice.java)
- class `AlphaHorizontalRibbon` (AlphaHorizontalRibbon.java)
- class `AlphaUiSheet` (AlphaUiSheet.java)

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

- `AlphaGridNineSlice.java`
- `AlphaHorizontalRibbon.java`
- `AlphaUiSheet.java`
- `GdxUiElementRenderer.java`
- `GdxUiPainter.java`
- `NineSlice.java`
- `NineSliceRenderer.java`
- `RegionMetrics.java`
- `TextAlign.java`
- `TextureRegionMetrics.java`
- `UiAnchor.java`
- `UiBindingSource.java`
- `UiColor.java`
- `UiDefinitionLoader.java`
- `UiDocument.java`
- `UiDocumentRenderer.java`
- `UiElementSkin.java`
- `UiFontDefinition.java`
- `UiJson.java`
- `UiLayout.java`
- `UiLayoutResolver.java`
- `UiNineSliceDefinition.java`
- `UiPainter.java`
- `UiPanelDefinition.java`
- `UiPanelLayout.java`
- `UiRect.java`
- `UiRegionDefinition.java`
- `UiTheme.java`
- `UiThemeDefinition.java`
- `UiWidgetDefinition.java`
