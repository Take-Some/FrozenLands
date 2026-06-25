package dev.takesome.helix.ui.markup.provider;

import com.badlogic.gdx.files.FileHandle;
import dev.takesome.helix.ui.markup.UiMarkupCompiler;
import dev.takesome.helix.ui.markup.UiMarkupDocument;
import dev.takesome.helix.ui.markup.UiMarkupParser;
import dev.takesome.helix.ui.markup.UiMarkupProvider;
import dev.takesome.helix.ui.css.UiIntrinsicTextMeasurer;
import dev.takesome.helix.data.io.DataFiles;
import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.runtime.EngineUiRuntime;
import dev.takesome.helix.ui.runtime.UiRuntimeInspectionSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Default HELIX UI Markup provider. */
public final class DefaultUiMarkupProvider implements UiMarkupProvider {
    private static final int DOCUMENT_CACHE_LIMIT = Math.max(0, Integer.getInteger("helix.ui.markup.cache.documents", 64));
    private static final Map<String, CachedDocument> DOCUMENT_CACHE = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedDocument> eldest) {
            return DOCUMENT_CACHE_LIMIT > 0 && size() > DOCUMENT_CACHE_LIMIT;
        }
    };

    private final UiMarkupParser parser = new UiMarkupParser();
    private final UiIntrinsicTextMeasurer textMeasurer;
    private final EngineI18n i18n;
    private final UiBindingSource bindingSource;
    private UiRuntimeInspectionSource lastInspectionSource = UiRuntimeInspectionSource.empty();

    public DefaultUiMarkupProvider() {
        this(null);
    }

    public DefaultUiMarkupProvider(UiIntrinsicTextMeasurer textMeasurer) {
        this(textMeasurer, null);
    }

    public DefaultUiMarkupProvider(UiIntrinsicTextMeasurer textMeasurer, EngineI18n i18n) {
        this(textMeasurer, i18n, null);
    }

    public DefaultUiMarkupProvider(UiIntrinsicTextMeasurer textMeasurer, EngineI18n i18n, UiBindingSource bindingSource) {
        this.textMeasurer = textMeasurer;
        this.i18n = i18n;
        this.bindingSource = bindingSource;
    }

    public static void clearRuntimeCaches() {
        synchronized (DOCUMENT_CACHE) {
            DOCUMENT_CACHE.clear();
        }
    }

    public UiRuntimeInspectionSource lastInspectionSource() {
        return lastInspectionSource == null ? UiRuntimeInspectionSource.empty() : lastInspectionSource;
    }

    public static CompletableFuture<UiMarkupDocument> preloadDocumentAsync(String path) {
        String cleanPath = DataFiles.normalize(path);
        return EngineUiRuntime.current().threadPool().supply(
                "ui-markup-preload:" + cleanPath,
                () -> preloadDocument(cleanPath)
        );
    }

    public static UiMarkupDocument preloadDocument(String path) {
        return new DefaultUiMarkupProvider().loadDocument(path);
    }

    @Override
    public UiMarkupDocument parse(String source) {
        return parser.parse(source);
    }

    @Override
    public UiMarkupDocument parse(String source, String sourcePath) {
        return parser.parse(source, sourcePath);
    }

    @Override
    public Node compile(UiMarkupDocument document, float width, float height, EventBus events) {
        UiMarkupCompiler compiler = new UiMarkupCompiler(events, textMeasurer, i18n, bindingSource);
        Node node = compiler.compile(document, width, height);
        lastInspectionSource = compiler.lastInspectionSource();
        return node;
    }

    @Override
    public Node load(String path, float width, float height, EventBus events) {
        UiMarkupDocument document = loadDocument(path);
        return compile(document, width, height, events);
    }

    private UiMarkupDocument loadDocument(String path) {
        String cleanPath = DataFiles.normalize(path);
        if (cleanPath.isBlank()) throw new IllegalArgumentException("markup path must not be blank");

        FileStamp stamp = fileStamp(cleanPath);
        if (stamp.cacheable() && DOCUMENT_CACHE_LIMIT > 0) {
            synchronized (DOCUMENT_CACHE) {
                CachedDocument cached = DOCUMENT_CACHE.get(cleanPath);
                if (cached != null && cached.stamp().equals(stamp)) {
                    return cached.document();
                }
            }
        }

        UiMarkupDocument document = parser.parse(DataFiles.readString(cleanPath), cleanPath);
        if (stamp.cacheable() && DOCUMENT_CACHE_LIMIT > 0) {
            synchronized (DOCUMENT_CACHE) {
                DOCUMENT_CACHE.put(cleanPath, new CachedDocument(stamp, document));
            }
        }
        return document;
    }

    private static FileStamp fileStamp(String path) {
        try {
            FileHandle file = DataFiles.file(path);
            if (file != null && file.exists()) {
                return new FileStamp(file.lastModified(), file.length(), true);
            }
        } catch (RuntimeException ignored) {
            // Some virtual/classpath resources do not expose a stable mtime through FileHandle.
        }
        return new FileStamp(-1L, -1L, false);
    }

    private record CachedDocument(FileStamp stamp, UiMarkupDocument document) {
    }

    private record FileStamp(long modified, long length, boolean cacheable) {
    }
}
