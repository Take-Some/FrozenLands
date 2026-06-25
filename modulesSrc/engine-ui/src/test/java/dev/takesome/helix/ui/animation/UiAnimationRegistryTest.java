package dev.takesome.helix.ui.animation;

import dev.takesome.helix.ui.animation.effects.FadeTextAnimation;
import dev.takesome.helix.ui.animation.effects.SlideTextAnimation;
import dev.takesome.helix.ui.animation.effects.TypewriterTextAnimation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiAnimationRegistryTest {
    @Test
    void defaultRegistryExposesConcreteAnimationClassesAndAliases() {
        UiAnimationRegistry registry = DefaultTextAnimations.registry();

        assertInstanceOf(TypewriterTextAnimation.class, registry.find("typewriter").orElseThrow());
        assertInstanceOf(FadeTextAnimation.class, registry.find("fade").orElseThrow());
        assertInstanceOf(SlideTextAnimation.class, registry.find("slide").orElseThrow());
        assertSame(registry.find("typewriter").orElseThrow(), registry.find("typing").orElseThrow());
        assertSame(registry.find("fade").orElseThrow(), registry.find("fade-in").orElseThrow());
        assertSame(registry.find("slide").orElseThrow(), registry.find("sliding").orElseThrow());
    }

    @Test
    void descriptorRegistrationKeepsOneCanonicalEffectPerAnimation() {
        UiAnimationRegistry registry = new UiAnimationRegistry()
                .register(UiAnimationDescriptor.of(new FadeTextAnimation(), "fade-in", "fading"));

        assertEquals(1, registry.effects().size());
        assertTrue(registry.ids().contains("fade"));
        assertTrue(registry.ids().contains("fade-in"));
        assertTrue(registry.ids().contains("fading"));
    }
    @Test
    void manifestLoaderInstallsBuiltInResourcePack() {
        UiAnimationDescriptorLoader loader = new UiAnimationDescriptorLoader(DefaultTextAnimations.class.getClassLoader());
        UiAnimationRegistry registry = loader.installResource(new UiAnimationRegistry(), DefaultTextAnimations.MANIFEST_RESOURCE);

        assertInstanceOf(TypewriterTextAnimation.class, registry.find("typewriter").orElseThrow());
        assertSame(registry.find("typewriter").orElseThrow(), registry.find("type").orElseThrow());
    }

    @Test
    void manifestLoaderInstallsCustomJsonPack() {
        UiAnimationDescriptorLoader loader = new UiAnimationDescriptorLoader(DefaultTextAnimations.class.getClassLoader());
        UiAnimationManifest manifest = loader.loadJson("""
                {
                  "id": "test.pack",
                  "animations": [
                    {
                      "id": "fade",
                      "className": "dev.takesome.helix.ui.animation.effects.FadeTextAnimation",
                      "aliases": ["soft-fade"]
                    }
                  ]
                }
                """);

        UiAnimationRegistry registry = loader.install(new UiAnimationRegistry(), manifest);

        assertInstanceOf(FadeTextAnimation.class, registry.find("fade").orElseThrow());
        assertSame(registry.find("fade").orElseThrow(), registry.find("soft-fade").orElseThrow());
    }

    @Test
    void validatorRejectsDuplicateIdsAndAliases() {
        UiAnimationManifest manifest = UiAnimationManifest.of(
                "broken.pack",
                UiAnimationManifest.animation("pulse", PulseTextAnimation.class, "same"),
                UiAnimationManifest.animation("same", PulseTextAnimation.class)
        );

        assertThrows(IllegalArgumentException.class, () -> new UiAnimationManifestValidator().validate(manifest));
    }

    @Test
    void loaderReloadsNamespaceAtomicallyAndRemovesOldAliases() {
        UiAnimationDescriptorLoader loader = new UiAnimationDescriptorLoader(DefaultTextAnimations.class.getClassLoader());
        UiAnimationRegistry registry = DefaultTextAnimations.registry();
        UiAnimationManifest first = loader.loadJson("""
                {
                  "id": "custom.pack",
                  "namespace": "custom.pack",
                  "animations": [
                    {
                      "id": "pulse",
                      "className": "dev.takesome.helix.ui.animation.UiAnimationRegistryTest$PulseTextAnimation",
                      "aliases": ["soft-pulse"]
                    }
                  ]
                }
                """);
        UiAnimationManifest second = loader.loadJson("""
                {
                  "id": "custom.pack",
                  "namespace": "custom.pack",
                  "animations": [
                    {
                      "id": "pulse",
                      "className": "dev.takesome.helix.ui.animation.UiAnimationRegistryTest$PulseTextAnimation",
                      "aliases": ["hard-pulse"]
                    }
                  ]
                }
                """);

        loader.reload(registry, first);
        assertTrue(registry.find("soft-pulse").isPresent());
        assertEquals("custom.pack", registry.namespaceOf("pulse"));

        loader.reload(registry, second);
        assertFalse(registry.find("soft-pulse").isPresent());
        assertTrue(registry.find("hard-pulse").isPresent());
        assertEquals("custom.pack", registry.namespaceOf("hard-pulse"));
    }

    @Test
    void disabledManifestUninstallsNamespace() {
        UiAnimationDescriptorLoader loader = new UiAnimationDescriptorLoader(DefaultTextAnimations.class.getClassLoader());
        UiAnimationRegistry registry = new UiAnimationRegistry();
        UiAnimationManifest enabled = UiAnimationManifest.of(
                "temporary.pack",
                UiAnimationManifest.animation("pulse", PulseTextAnimation.class, "pulse-alias")
        );
        UiAnimationManifest disabled = new UiAnimationManifest();
        disabled.id = "temporary.pack";
        disabled.namespace = "temporary.pack";
        disabled.enabled = false;

        loader.reload(registry, enabled);
        assertTrue(registry.find("pulse-alias").isPresent());

        loader.reload(registry, disabled);
        assertFalse(registry.find("pulse-alias").isPresent());
        assertTrue(registry.idsInNamespace("temporary.pack").isEmpty());
    }

    public static final class PulseTextAnimation implements UiTextAnimationEffect {
        @Override
        public String id() {
            return "pulse";
        }

        @Override
        public void apply(UiTextAnimationDefinition definition, UiTextAnimationRuntime runtime, UiTextAnimationFrame frame) {
            if (frame != null) frame.alpha *= 0.5f;
        }
    }

}
