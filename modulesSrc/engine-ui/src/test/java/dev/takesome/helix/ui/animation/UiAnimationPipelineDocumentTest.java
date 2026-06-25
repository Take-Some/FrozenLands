package dev.takesome.helix.ui.animation;

import dev.takesome.helix.ui.definition.UiDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiAnimationPipelineDocumentTest {
    @Test
    void documentInlinePackConfiguresPipelineRegistry() {
        UiDocument document = new UiDocument();
        document.animationPacks.add(UiAnimationManifest.of(
                "document.animations",
                UiAnimationManifest.animation("pulse", UiAnimationRegistryTest.PulseTextAnimation.class, "doc-pulse")
        ));
        UiAnimationPipeline pipeline = new UiAnimationPipeline().configure(document);

        assertTrue(pipeline.registry().find("doc-pulse").isPresent());
        assertSame(pipeline.registry().find("pulse").orElseThrow(), pipeline.registry().find("doc-pulse").orElseThrow());
    }

    @Test
    void themeManifestReferenceConfiguresPipelineRegistry() {
        UiDocument document = new UiDocument();
        document.theme.animationManifests.add("resource:" + DefaultTextAnimations.MANIFEST_RESOURCE);
        UiAnimationPipeline pipeline = UiAnimationPipelineFactory.fromDocument(document);

        assertTrue(pipeline.registry().find("typewriter").isPresent());
        assertTrue(pipeline.registry().find("fade-in").isPresent());
    }

    @Test
    void documentReconfigureReloadsRemovedPackNamespace() {
        UiDocument first = new UiDocument();
        first.animationPacks.add(UiAnimationManifest.of(
                "document.animations",
                UiAnimationManifest.animation("pulse", UiAnimationRegistryTest.PulseTextAnimation.class, "doc-pulse")
        ));
        UiDocument second = new UiDocument();
        UiAnimationManifest disabled = new UiAnimationManifest();
        disabled.id = "document.animations";
        disabled.namespace = "document.animations";
        disabled.enabled = false;
        second.animationPacks.add(disabled);

        UiAnimationPipeline pipeline = new UiAnimationPipeline().configure(first);
        assertTrue(pipeline.registry().find("doc-pulse").isPresent());

        pipeline.configure(second);
        assertFalse(pipeline.registry().find("doc-pulse").isPresent());
    }
}
