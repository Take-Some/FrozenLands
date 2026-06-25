# helix/ui/icons

Typed icon-font layer for retained UI.

## Responsibility

- Keep icon glyph ids out of UI/render/toast business code.
- Provide a stable icon registry: `IconRegistry`.
- Provide a built-in Font Awesome solid subset: `FontAwesomeSolidIcon`.
- Provide a classpath descriptor for the bundled font file.
- Provide LibGDX font generation through `GdxIconFontFactory`.

## Resource contract

The bundled font is expected here:

```text
core/src/main/resources/dev/takesome/helix/ui/icons/fontawesome/fa-solid-900.ttf
```

Code should reference it through:

```java
UiIconFontResources.FONT_AWESOME_SOLID
```

## Usage

```java
IconRegistry icons = UiIconRegistries.standard();
UiIcon gear = icons.require("gear");
UiIcon info = icons.require("fontawesome:solid:circle-info");
```

For LibGDX:

```java
BitmapFont iconFont = GdxIconFontFactory.createFontAwesomeSolid(24);
```

## License note

The uploaded file is Font Awesome Pro Solid. Keep it out of public repositories unless the project has a valid Font Awesome Pro license and the distribution model permits bundling it.
