package dev.takesome.helix.ui.animation;

import java.util.Arrays;
import java.util.List;

/**
 * Registration descriptor for one UI text animation effect and its aliases.
 */
public record UiAnimationDescriptor(
        UiTextAnimationEffect effect,
        List<String> aliases
) {
    public UiAnimationDescriptor {
        if (effect == null) throw new IllegalArgumentException("effect must not be null");
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    public static UiAnimationDescriptor of(UiTextAnimationEffect effect, String... aliases) {
        return new UiAnimationDescriptor(effect, aliases == null ? List.of() : Arrays.asList(aliases));
    }
}
