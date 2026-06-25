package dev.takesome.helix.ui.animation;

import java.util.List;

/** Built-in UI text animations registered by the default pipeline. */
public final class DefaultTextAnimations {
    public static final String MANIFEST_RESOURCE = "dev/takesome/helix/ui/animation/builtin-text-animations.json";

    private static final UiAnimationDescriptorLoader LOADER = new UiAnimationDescriptorLoader(DefaultTextAnimations.class.getClassLoader());

    private DefaultTextAnimations() {
    }

    public static UiAnimationManifest manifest() {
        return LOADER.loadResource(MANIFEST_RESOURCE);
    }

    public static List<UiAnimationDescriptor> descriptors() {
        return LOADER.descriptors(manifest());
    }

    public static UiAnimationRegistry registry() {
        return registerInto(new UiAnimationRegistry());
    }

    public static UiAnimationRegistry registerInto(UiAnimationRegistry registry) {
        return LOADER.install(registry, manifest());
    }
}
