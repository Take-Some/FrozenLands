package dev.takesome.helix.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitiveManifest;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitivePipelineFactory;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitiveRegistryLoader;
import dev.takesome.helix.ui.primitives.UiWidgetPrimitiveRendererRegistry;
import dev.takesome.helix.ui.legacy.render.UiWidgetRenderContext;

final class UiWidgetPrimitiveRendererRegistryTest {
    @Test
    void registryDispatchesRendererByIdAndAlias() {
        AtomicInteger calls = new AtomicInteger();
        UiWidgetPrimitiveRendererRegistry registry = new UiWidgetPrimitiveRendererRegistry()
                .register("custom", context -> calls.incrementAndGet())
                .alias("custom-alias", "custom");

        assertTrue(registry.render("custom", context("direct")));
        assertTrue(registry.render("CUSTOM_ALIAS", context("alias")));
        assertEquals(2, calls.get());
    }

    @Test
    void registryReportsUnknownPrimitiveWithoutThrowing() {
        UiWidgetPrimitiveRendererRegistry registry = new UiWidgetPrimitiveRendererRegistry();

        assertFalse(registry.render("missing", context("missing")));
    }


    @Test
    void loaderReloadsPrimitiveAliasesFromManifest() {
        AtomicInteger calls = new AtomicInteger();
        UiWidgetPrimitiveRendererRegistry registry = new UiWidgetPrimitiveRendererRegistry()
                .register("base", "text", context -> calls.incrementAndGet());
        UiWidgetPrimitiveManifest manifest = new UiWidgetPrimitiveRegistryLoader().loadJson("""
                {
                  "id": "custom.primitives",
                  "namespace": "custom.primitives",
                  "primitives": [
                    {"id":"headline", "renderer":"text", "aliases":["headline-text"]}
                  ]
                }
                """);

        new UiWidgetPrimitiveRegistryLoader().reload(registry, manifest);

        assertTrue(registry.render("headline-text", context("headline")));
        assertEquals("custom.primitives", registry.namespaceOf("headline-text"));
        assertEquals(1, calls.get());
    }

    @Test
    void disabledManifestUnregistersPrimitiveNamespace() {
        AtomicInteger calls = new AtomicInteger();
        UiWidgetPrimitiveRegistryLoader loader = new UiWidgetPrimitiveRegistryLoader();
        UiWidgetPrimitiveRendererRegistry registry = new UiWidgetPrimitiveRendererRegistry()
                .register("base", "text", context -> calls.incrementAndGet());
        UiWidgetPrimitiveManifest enabled = UiWidgetPrimitiveManifest.of(
                "temporary.primitives",
                UiWidgetPrimitiveManifest.primitive("headline", "text", "headline-text")
        );
        UiWidgetPrimitiveManifest disabled = new UiWidgetPrimitiveManifest();
        disabled.id = "temporary.primitives";
        disabled.namespace = "temporary.primitives";
        disabled.enabled = false;

        loader.reload(registry, enabled);
        assertTrue(registry.render("headline-text", context("enabled")));

        loader.reload(registry, disabled);
        assertFalse(registry.render("headline-text", context("disabled")));
        assertTrue(registry.idsInNamespace("temporary.primitives").isEmpty());
        assertTrue(registry.render("text", context("base-still-installed")));
    }

    @Test
    void documentDeclaredPrimitivePackAddsAliasThroughFactory() {
        AtomicInteger calls = new AtomicInteger();
        UiWidgetPrimitiveRendererRegistry registry = new UiWidgetPrimitiveRendererRegistry()
                .register("base", "text", context -> calls.incrementAndGet());
        UiDocument document = new UiDocument();
        document.primitivePacks.add(UiWidgetPrimitiveManifest.of(
                "document.primitives",
                UiWidgetPrimitiveManifest.primitive("headline", "text", "headline-text")
        ));

        UiWidgetPrimitivePipelineFactory.reloadInto(registry, document, new UiWidgetPrimitiveRegistryLoader());

        assertTrue(registry.render("headline", context("document")));
        assertTrue(registry.render("headline-text", context("document-alias")));
        assertEquals(2, calls.get());
    }
    @Test
    void normalizeSupportsCaseAndUnderscoreAliases() {
        assertEquals("stretch-image", UiWidgetPrimitiveRendererRegistry.normalize("Stretch_Image"));
    }

    private static UiWidgetRenderContext context(String key) {
        return new UiWidgetRenderContext(null, null, null, new UiRect(0f, 0f, 1f, 1f), new UiWidgetDefinition(), key, 0f);
    }
}
