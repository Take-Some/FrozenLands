package dev.takesome.helix.ui.markup;

import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.ui.markup.provider.DefaultUiMarkupProvider;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.SceneNode;
import dev.takesome.helix.ui.runtime.UiRuntimeInspectionSource;

/** Loads markup documents into retained UI scene roots. */
public final class UiMarkupSceneLoader {
    private final UiMarkupProvider provider;
    private UiRuntimeInspectionSource lastInspectionSource = UiRuntimeInspectionSource.empty();

    public UiMarkupSceneLoader() {
        this(new DefaultUiMarkupProvider());
    }

    public UiMarkupSceneLoader(EngineI18n i18n) {
        this(new DefaultUiMarkupProvider(null, i18n));
    }

    public UiMarkupSceneLoader(UiMarkupProvider provider) {
        this.provider = provider == null ? new DefaultUiMarkupProvider() : provider;
    }

    public static void clearRuntimeCaches() {
        DefaultUiMarkupProvider.clearRuntimeCaches();
        UiMarkupCompiler.clearRuntimeCaches();
    }

    public UiRuntimeInspectionSource lastInspectionSource() {
        return lastInspectionSource == null ? UiRuntimeInspectionSource.empty() : lastInspectionSource;
    }

    public Node loadInto(SceneNode root, String path, EventBus events) {
        Node node = provider.loadInto(root, path, events);
        if (provider instanceof DefaultUiMarkupProvider defaultProvider) {
            lastInspectionSource = defaultProvider.lastInspectionSource();
        }
        return node;
    }

    public Node loadDocumentInto(SceneNode root, UiMarkupDocument document, EventBus events) {
        if (root == null) throw new IllegalArgumentException("root must not be null");
        if (document == null) throw new IllegalArgumentException("document must not be null");
        Node node = provider.compile(document, root.sceneWidth(), root.sceneHeight(), events);
        root.add(node);
        if (provider instanceof DefaultUiMarkupProvider defaultProvider) {
            lastInspectionSource = defaultProvider.lastInspectionSource();
        }
        return node;
    }

    public Node loadSourceInto(SceneNode root, String source, EventBus events) {
        return loadSourceInto(root, source, "", events);
    }

    public Node loadSourceInto(SceneNode root, String source, String sourcePath, EventBus events) {
        if (root == null) throw new IllegalArgumentException("root must not be null");
        UiMarkupDocument document = provider.parse(source == null ? "" : source, sourcePath == null ? "" : sourcePath);
        Node node = provider.compile(document, root.sceneWidth(), root.sceneHeight(), events);
        root.add(node);
        if (provider instanceof DefaultUiMarkupProvider defaultProvider) {
            lastInspectionSource = defaultProvider.lastInspectionSource();
        }
        return node;
    }
}
