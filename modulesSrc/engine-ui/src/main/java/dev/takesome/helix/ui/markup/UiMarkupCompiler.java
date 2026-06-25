package dev.takesome.helix.ui.markup;

import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.css.UiIntrinsicTextMeasurer;
import dev.takesome.helix.ui.markup.internal.compile.UiDomCompilationPipeline;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.runtime.UiRuntimeInspectionSource;

/** Public facade for compiling HELIX UI Markup into the retained UI node tree. */
public final class UiMarkupCompiler {
    private final UiDomCompilationPipeline pipeline;

    public UiMarkupCompiler(EventBus events) {
        this(events, null);
    }

    public UiMarkupCompiler(EventBus events, UiIntrinsicTextMeasurer textMeasurer) {
        this(events, textMeasurer, null);
    }

    public UiMarkupCompiler(EventBus events, UiIntrinsicTextMeasurer textMeasurer, EngineI18n i18n) {
        this(events, textMeasurer, i18n, null);
    }

    public UiMarkupCompiler(EventBus events, UiIntrinsicTextMeasurer textMeasurer, EngineI18n i18n, UiBindingSource bindingSource) {
        this.pipeline = new UiDomCompilationPipeline(events, textMeasurer, i18n, bindingSource);
    }

    public static void clearRuntimeCaches() {
        UiDomCompilationPipeline.clearRuntimeCaches();
    }

    public UiRuntimeInspectionSource lastInspectionSource() {
        return pipeline.lastInspectionSource();
    }

    public Node compile(UiMarkupDocument document, float width, float height) {
        return pipeline.compile(document, width, height);
    }
}
