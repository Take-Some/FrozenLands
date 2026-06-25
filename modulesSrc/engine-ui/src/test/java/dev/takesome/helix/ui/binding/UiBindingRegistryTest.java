package dev.takesome.helix.ui.binding;

import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiDocument;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiBindingRegistryTest {
    @Test
    void validatorRejectsDuplicateTargets() {
        UiBindingManifest manifest = UiBindingManifest.of(
                "broken.bindings",
                UiBindingManifest.binding("a", "xpText.text", "state.a", "string", null),
                UiBindingManifest.binding("b", "xpText.text", "state.b", "string", null)
        );

        assertThrows(IllegalArgumentException.class, () -> new UiBindingManifestValidator().validate(manifest));
    }

    @Test
    void loaderReloadsNamespaceAndRemovesOldTargets() {
        UiBindingDescriptorLoader loader = new UiBindingDescriptorLoader();
        UiBindingRegistry registry = new UiBindingRegistry();
        UiBindingManifest first = loader.loadJson("""
                {
                  "id": "hud.bindings",
                  "namespace": "hud.bindings",
                  "bindings": [
                    {"id":"xp.current", "target":"xpText.text", "source":"player.xp", "type":"number", "format":"XP {value}"}
                  ]
                }
                """);
        UiBindingManifest second = loader.loadJson("""
                {
                  "id": "hud.bindings",
                  "namespace": "hud.bindings",
                  "bindings": [
                    {"id":"xp.next", "target":"xpNextText.text", "source":"player.xpNext", "type":"number", "format":"/ {value}"}
                  ]
                }
                """);

        loader.reload(registry, first);
        assertTrue(registry.findTarget("xpText.text").isPresent());
        assertEquals("hud.bindings", registry.namespaceOf("xpText.text"));

        loader.reload(registry, second);
        assertFalse(registry.findTarget("xpText.text").isPresent());
        assertTrue(registry.findTarget("xpNextText.text").isPresent());
    }


    @Test
    void runtimeSourceMissingVisibilityDescriptorDefaultsVisible() {
        UiBindingRuntimeSource runtime = new UiBindingRuntimeSource(new MapBindingSource(Map.of()), new UiBindingRegistry());

        assertTrue(runtime.boolTarget("player.visible", null));
        assertTrue(runtime.boolTarget("player.visible", ""));
    }

    @Test
    void expressionEvaluatorComputesSafeNumericDsl() {
        UiBindingExpressionEvaluator evaluator = new UiBindingExpressionEvaluator();
        MapBindingSource source = new MapBindingSource(Map.of("player.xp", "42", "player.xpNext", "100"));

        assertEquals(0.42f, evaluator.number("player.xp / max(player.xpNext, 1)", source, -1f), 0.0001f);
        assertEquals(1.0f, evaluator.number("clamp(player.xp / 10, 0, 1)", source, -1f), 0.0001f);
    }

    @Test
    void expressionEvaluatorComputesBooleanDsl() {
        UiBindingExpressionEvaluator evaluator = new UiBindingExpressionEvaluator();
        MapBindingSource source = new MapBindingSource(Map.of("player.xp", "42", "player.xpNext", "100"));

        assertFalse(evaluator.bool("player.xp >= player.xpNext", source, true));
        assertTrue(evaluator.bool("player.xp < player.xpNext && max(player.xp, 0) == 42", source, false));
    }

    @Test
    void expressionEvaluatorSupportsBooleanParenthesesAndBareBoolPaths() {
        UiBindingExpressionEvaluator evaluator = new UiBindingExpressionEvaluator();
        MapBindingSource closed = new MapBindingSource(Map.of(
                "controlsOpen", "false",
                "game.paused", "false",
                "debug.open", "false",
                "ui.menu.open", "false"
        ));
        MapBindingSource open = new MapBindingSource(Map.of(
                "controlsOpen", "true",
                "game.paused", "false",
                "debug.open", "false",
                "ui.menu.open", "false"
        ));

        assertTrue(evaluator.bool("!(controlsOpen || game.paused || debug.open || ui.menu.open)", closed, false));
        assertFalse(evaluator.bool("!(controlsOpen || game.paused || debug.open || ui.menu.open)", open, true));
        assertTrue(evaluator.bool("!ui.menu.open && (controlsOpen || game.paused || debug.open)", open, false));
    }

    @Test
    void validatorAcceptsHudVisibilityExpressions() {
        UiBindingDescriptor collapsed = UiBindingManifest.binding("hud.controls.collapsed.visible", "controlsC.visible", null, "boolean", null);
        collapsed.expr = "!(controlsOpen || game.paused || debug.open || ui.menu.open)";
        UiBindingDescriptor expanded = UiBindingManifest.binding("hud.controls.expanded.visible", "controlsE.visible", null, "boolean", null);
        expanded.expr = "!ui.menu.open && (controlsOpen || game.paused || debug.open)";

        new UiBindingManifestValidator().validate(UiBindingManifest.of("hud", collapsed, expanded));
    }
    @Test
    void runtimeSourceResolvesExpressionWithFormat() {
        UiBindingDescriptor descriptor = UiBindingManifest.binding("xp.ratio", "xpBar.progress", null, "progress", "{value}");
        descriptor.expr = "player.xp / max(player.xpNext, 1)";
        UiBindingRegistry registry = new UiBindingRegistry().register("hud.bindings", descriptor);
        UiBindingRuntimeSource runtime = new UiBindingRuntimeSource(new MapBindingSource(Map.of("player.xp", "42", "player.xpNext", "100")), registry);

        assertEquals(0.42f, runtime.numberTarget("xpBar.progress", null), 0.0001f);
        assertEquals("0.42", runtime.textTarget("xpBar.progress", "fallback"));
    }

    @Test
    void runtimeSourceResolvesTargetWithFormat() {
        UiBindingRegistry registry = new UiBindingRegistry().register(
                "hud.bindings",
                UiBindingManifest.binding("xp.current", "xpText.text", "player.xp", "number", "XP {value}")
        );
        UiBindingRuntimeSource runtime = new UiBindingRuntimeSource(new MapBindingSource(Map.of("player.xp", "42")), registry);

        assertEquals("XP 42", runtime.textTarget("xpText.text", "fallback"));
    }

    @Test
    void documentBindingPackBuildsRegistry() {
        UiDocument document = new UiDocument();
        document.bindingPacks.add(UiBindingManifest.of(
                "document.bindings",
                UiBindingManifest.binding("health.text", "healthText.text", "player.health", "number", "HP {value}")
        ));

        UiBindingRegistry registry = UiBindingPipelineFactory.fromDocument(document);

        assertTrue(registry.findTarget("healthText.text").isPresent());
        assertEquals("document.bindings", registry.namespaceOf("healthText.text"));
    }

    @Test
    void disabledManifestUninstallsNamespace() {
        UiBindingDescriptorLoader loader = new UiBindingDescriptorLoader();
        UiBindingRegistry registry = new UiBindingRegistry();
        UiBindingManifest enabled = UiBindingManifest.of(
                "temporary.bindings",
                UiBindingManifest.binding("tmp", "tmpText.text", "tmp.value", "string", null)
        );
        UiBindingManifest disabled = new UiBindingManifest();
        disabled.id = "temporary.bindings";
        disabled.namespace = "temporary.bindings";
        disabled.enabled = false;

        loader.reload(registry, enabled);
        assertTrue(registry.findTarget("tmpText.text").isPresent());

        loader.reload(registry, disabled);
        assertFalse(registry.findTarget("tmpText.text").isPresent());
        assertTrue(registry.targetsInNamespace("temporary.bindings").isEmpty());
    }

    private static final class MapBindingSource implements UiBindingSource {
        private final Map<String, String> values = new HashMap<>();

        private MapBindingSource(Map<String, String> values) {
            if (values != null) this.values.putAll(values);
        }

        @Override
        public String text(String key) {
            return values.getOrDefault(key, "");
        }

        @Override
        public float number(String key) {
            String value = text(key);
            if (value.isBlank()) return 0f;
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException ex) {
                return Boolean.parseBoolean(value) ? 1f : 0f;
            }
        }

        @Override
        public boolean bool(String key) {
            return Boolean.parseBoolean(text(key));
        }
    }
}
