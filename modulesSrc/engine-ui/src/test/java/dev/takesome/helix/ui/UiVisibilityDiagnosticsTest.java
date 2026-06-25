package dev.takesome.helix.ui;

import dev.takesome.helix.ui.binding.UiBindingRegistry;
import dev.takesome.helix.ui.binding.UiBindingRuntimeSource;
import dev.takesome.helix.ui.binding.UiBindingDescriptor;
import dev.takesome.helix.ui.binding.UiBindingManifest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.diagnostics.UiVisibilityDiagnostics;

final class UiVisibilityDiagnosticsTest {
    @Test
    void missingTargetWithoutFallbackDefaultsVisibleAndReportsMissingTarget() {
        UiVisibilityDiagnostics.Decision decision = UiVisibilityDiagnostics.evaluate("panel.visible", null, new UiBindingRuntimeSource(new MapSource(Map.of()), new UiBindingRegistry()));
        assertTrue(decision.visible());
        assertTrue(decision.missingTarget());
        assertEquals("missing-target-default-visible", decision.reason());
    }


    @Test
    void descriptorExpressionFalseReportsReason() {
        UiBindingDescriptor descriptor = UiBindingManifest.binding("panel.hidden", "panel.visible", null, "boolean", null);
        descriptor.expr = "ui.open";
        UiBindingRegistry registry = new UiBindingRegistry().register("hud", descriptor);
        UiVisibilityDiagnostics.Decision decision = UiVisibilityDiagnostics.evaluate("panel.visible", null, new UiBindingRuntimeSource(new MapSource(Map.of("ui.open", "false")), registry));
        assertFalse(decision.visible());
        assertTrue(decision.descriptorFound());
        assertEquals("panel.hidden", decision.descriptorId());
        assertEquals("ui.open", decision.expression());
        assertEquals("descriptor-expression-explicit-false", decision.reason());
    }

    @Test
    void fallbackKeyFalseReportsReason() {
        UiVisibilityDiagnostics.Decision decision = UiVisibilityDiagnostics.evaluate("widget.visible", "ui.flag", new MapSource(Map.of("ui.flag", "false")));
        assertFalse(decision.visible());
        assertFalse(decision.descriptorFound());
        assertEquals("ui.flag", decision.source());
        assertEquals("fallback-key-explicit-false", decision.reason());
    }

    private static final class MapSource implements UiBindingSource {
        private final Map<String, String> values;
        private MapSource(Map<String, String> values) { this.values = values; }
        @Override public String text(String key) { return values.getOrDefault(key, ""); }
        @Override public float number(String key) { String value = text(key); return value.isBlank() ? 0f : Float.parseFloat(value); }
        @Override public boolean bool(String key) { return Boolean.parseBoolean(text(key)); }
    }
}
