package dev.takesome.helix.ui.icons.fontawesome;

import dev.takesome.helix.ui.icons.UiIconRegistries;
import dev.takesome.helix.ui.icons.registry.IconRegistry;
import dev.takesome.helix.ui.icons.resources.UiIconFontResources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FontAwesomeBundleTest {
    @Test
    void bundledRegistryIsComposedFromStyles() {
        IconRegistry registry = FontAwesomeBundle.registry();
        int expected = FontAwesomeStyle.SOLID.icons().size()
                + FontAwesomeStyle.REGULAR.icons().size()
                + FontAwesomeStyle.BRANDS.icons().size();

        assertEquals(expected, registry.size());
        assertSame(FontAwesomeSolidIcon.GEAR, registry.require("fontawesome:solid:gear"));
        assertSame(FontAwesomeRegularIcon.BELL, registry.require("fontawesome:regular:bell"));
        assertSame(FontAwesomeBrandIcon.GITHUB, registry.require("fontawesome:brands:github"));
    }

    @Test
    void stylesExposeRegistryAndFontResources() {
        assertSame(UiIconFontResources.FONT_AWESOME_SOLID, FontAwesomeStyle.SOLID.resource());
        assertSame(UiIconFontResources.FONT_AWESOME_REGULAR, FontAwesomeStyle.REGULAR.resource());
        assertSame(UiIconFontResources.FONT_AWESOME_BRANDS, FontAwesomeStyle.BRANDS.resource());
        assertSame(FontAwesomeSolidIcon.CIRCLE_INFO, FontAwesomeStyle.SOLID.registry().require("circle-info"));
    }

    @Test
    void publicRegistryFacadeUsesBundleAbstraction() {
        assertEquals(FontAwesomeBundle.registry().size(), UiIconRegistries.fontAwesomeBundled().size());
        assertTrue(FontAwesomeStyle.find("solid").isPresent());
        assertSame(FontAwesomeStyle.BRANDS, FontAwesomeStyle.require("brands"));
    }
    @Test
    void fontResourceFacadeDerivesBundledResourcesFromStyles() {
        assertEquals(
                FontAwesomeBundle.styles().stream().map(FontAwesomeStyle::resource).toList(),
                UiIconFontResources.fontAwesomeBundled()
        );
        assertEquals(FontAwesomeStyle.SOLID.resource().classpathPath(), UiIconFontResources.FONT_AWESOME_SOLID_PATH);
        assertEquals(FontAwesomeStyle.REGULAR.resource().classpathPath(), UiIconFontResources.FONT_AWESOME_REGULAR_PATH);
        assertEquals(FontAwesomeStyle.BRANDS.resource().classpathPath(), UiIconFontResources.FONT_AWESOME_BRANDS_PATH);
    }

    @Test
    void stylesAreDerivedFromEnumValues() {
        assertEquals(java.util.List.of(FontAwesomeStyle.values()), FontAwesomeBundle.styles());
    }

    @Test
    void styleIconsAreResolvedLazilyAndCached() {
        assertSame(FontAwesomeStyle.SOLID.icons(), FontAwesomeStyle.SOLID.icons());
        assertSame(FontAwesomeSolidIcon.GEAR, FontAwesomeStyle.SOLID.registry().require("fontawesome:solid:gear"));
    }

    @Test
    void iconFamilyDescriptorOwnsListAndRegistryBoilerplate() {
        FontAwesomeIconFamily<?> family = FontAwesomeStyle.SOLID.family();

        assertEquals("solid", family.styleId());
        assertEquals("fontawesome-solid", family.registryName());
        assertSame(family.icons(), family.icons());
        assertEquals(FontAwesomeSolidIcon.values().length, family.icons().size());
        assertSame(FontAwesomeSolidIcon.GEAR, family.registry().require("fontawesome:solid:gear"));
    }

}
