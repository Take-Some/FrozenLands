package dev.takesome.helix.ui.pipeline;

import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.binding.UiBindingDescriptorLoader;
import dev.takesome.helix.ui.binding.UiBindingPipelineFactory;
import dev.takesome.helix.ui.binding.UiBindingRegistry;

/** Prepares a runtime binding source from document/theme binding manifests. */
public final class UiBindingPreparePass {
    private final UiBindingRegistry registry = new UiBindingRegistry();
    private final UiBindingDescriptorLoader loader = new UiBindingDescriptorLoader();
    private String signature = "";
    private EngineI18n i18n;

    public UiBindingSource prepare(UiDocument document, UiBindingSource source) {
        String nextSignature = UiBindingPipelineFactory.signature(document);
        if (!nextSignature.equals(signature)) {
            UiBindingPipelineFactory.reloadInto(registry, document, loader);
            signature = nextSignature;
        }
        UiBindingSource fallback = source == null ? EmptySource.INSTANCE : source;
        return UiBindingPipelineFactory.runtimeSource(document, fallback, registry, loader, i18n);
    }

    public UiBindingRegistry registry() {
        return registry;
    }

    public void setI18n(EngineI18n i18n) {
        this.i18n = i18n;
    }

    private enum EmptySource implements UiBindingSource {
        INSTANCE;

        @Override
        public String text(String key) { return ""; }

        @Override
        public float number(String key) { return 0f; }

        @Override
        public boolean bool(String key) { return false; }
    }
}
