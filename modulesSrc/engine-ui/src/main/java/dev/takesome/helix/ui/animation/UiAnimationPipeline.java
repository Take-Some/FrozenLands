package dev.takesome.helix.ui.animation;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.definition.UiThemeDefinition;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.css.transition.UiCssTransitionDescriptor;
import dev.takesome.helix.ui.css.transition.UiCssTransitionTimeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Composes registered UI animation effects for one UI widget render pass. */
public final class UiAnimationPipeline {
    private final UiAnimationRegistry registry;
    private final UiAnimationDescriptorLoader descriptorLoader = new UiAnimationDescriptorLoader();
    private String documentAnimationSignature = "";
    private final Map<String, TextAnimationState> textStates = new HashMap<>();
    private final Set<String> activeTextKeys = new HashSet<>();
    private final Map<UiTextAnimationDefinition, List<String>> effectIdCache = new IdentityHashMap<>();
    private final UiCssTransitionTimeline cssTransitions = new UiCssTransitionTimeline();
    private float frameTime;

    public UiAnimationPipeline() {
        this(DefaultTextAnimations.registry());
    }

    public UiAnimationPipeline(UiAnimationRegistry registry) {
        this.registry = registry == null ? new UiAnimationRegistry() : registry;
    }

    public UiAnimationPipeline register(UiTextAnimationEffect effect) {
        registry.register(effect);
        return this;
    }

    public UiAnimationPipeline alias(String alias, String targetId) {
        registry.alias(alias, targetId);
        return this;
    }

    public UiAnimationRegistry registry() {
        return registry;
    }

    public UiCssTransitionTimeline cssTransitions() {
        return cssTransitions;
    }

    public Map<String, String> transitionStyle(String key, Map<String, String> targetStyle, List<UiCssTransitionDescriptor> transitions) {
        return cssTransitions.frame(key, targetStyle, transitions, Math.round(frameTime * 1000f));
    }

    public Map<String, String> transitionStyle(String key, Map<String, String> targetStyle, UiCssTransitionDescriptor transition) {
        return cssTransitions.frame(key, targetStyle, transition, Math.round(frameTime * 1000f));
    }

    public UiAnimationPipeline configure(UiDocument document) {
        String signature = UiAnimationPipelineFactory.signature(document);
        if (!signature.equals(documentAnimationSignature)) {
            UiAnimationPipelineFactory.reloadInto(registry, document, descriptorLoader);
            documentAnimationSignature = signature;
            reset();
        }
        return this;
    }

    public UiAnimationPipeline configure(UiThemeDefinition theme) {
        String signature = UiAnimationPipelineFactory.signature(theme);
        if (!signature.equals(documentAnimationSignature)) {
            UiAnimationPipelineFactory.reloadInto(registry, theme, descriptorLoader);
            documentAnimationSignature = signature;
            reset();
        }
        return this;
    }

    public void beginFrame(float uiTime) {
        frameTime = Float.isFinite(uiTime) ? uiTime : 0f;
        activeTextKeys.clear();
    }

    public void endFrame() {
        textStates.keySet().retainAll(activeTextKeys);
    }

    public void reset() {
        textStates.clear();
        activeTextKeys.clear();
        cssTransitions.reset();
        effectIdCache.clear();
        frameTime = 0f;
    }

    public boolean hasTextAnimation(UiWidgetDefinition widget) {
        return enabled(UiAnimationDefinitionResolver.text(widget));
    }

    public UiTextAnimationFrame animateText(UiWidgetDefinition widget, String key, String text) {
        UiTextAnimationDefinition definition = UiAnimationDefinitionResolver.text(widget);
        if (!enabled(definition)) return new UiTextAnimationFrame(text);

        String stateKey = key == null || key.isBlank()
                ? "text@" + System.identityHashCode(widget)
                : key;
        activeTextKeys.add(stateKey);

        String safeText = emptyIfNull(text);
        TextAnimationState state = textStates.computeIfAbsent(stateKey, ignored -> new TextAnimationState(frameTime, safeText));
        boolean resetOnTextChange = definition.bool("resetOnTextChange", definition.resetOnTextChange);
        if (resetOnTextChange && !state.text.equals(safeText)) state.startedAt = frameTime;
        state.text = safeText;

        return animate(definition, safeText, frameTime - state.startedAt);
    }

    public UiTextAnimationFrame animate(UiTextAnimationDefinition definition, String text, float elapsedSeconds) {
        if (text == null || text.isEmpty()) return new UiTextAnimationFrame("");
        if (!enabled(definition)) return new UiTextAnimationFrame(text);

        float delay = Math.max(0f, definition.number("delay", definition.delay));
        float local = elapsedSeconds - delay;
        if (local < 0f) return UiTextAnimationFrame.hidden();

        UiTextAnimationFrame frame = new UiTextAnimationFrame(text);
        for (String id : effectIdsCached(definition)) {
            UiTextAnimationEffect effect = registry.findOrNull(id);
            if (effect != null) effect.apply(definition, new UiTextAnimationRuntime(id, text, local), frame);
            if (!frame.visible || frame.alpha <= 0f || frame.text == null || frame.text.isEmpty()) break;
        }
        return frame;
    }

    private static boolean enabled(UiTextAnimationDefinition definition) {
        if (definition == null) return false;
        String expression = definition.effectExpression();
        return expression != null && !expression.isBlank() && !off(expression);
    }

    private static boolean off(String type) {
        String normalized = UiAnimationRegistry.normalize(type);
        return normalized.equals("none") || normalized.equals("off") || normalized.equals("false");
    }

    private List<String> effectIdsCached(UiTextAnimationDefinition definition) {
        if (definition == null) return List.of();
        return effectIdCache.computeIfAbsent(definition, UiAnimationPipeline::effectIds);
    }

    private static List<String> effectIds(UiTextAnimationDefinition definition) {
        ArrayList<String> out = new ArrayList<>();
        if (definition == null) return out;
        if (definition.effects != null && !definition.effects.isEmpty()) {
            for (String item : definition.effects) {
                String normalized = UiAnimationRegistry.normalize(item);
                if (!normalized.isBlank() && !off(normalized)) out.add(normalized);
            }
            return out;
        }
        return effectIds(definition.effectExpression());
    }

    private static List<String> effectIds(String raw) {
        ArrayList<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        String[] parts = raw.split("[+,|;\\s]+");
        for (String part : parts) {
            String normalized = UiAnimationRegistry.normalize(part);
            if (!normalized.isBlank() && !off(normalized)) out.add(normalized);
        }
        return out;
    }

    private static final class TextAnimationState {
        private float startedAt;
        private String text;

        private TextAnimationState(float startedAt, String text) {
            this.startedAt = startedAt;
            this.text = emptyIfNull(text);
        }
    }
}
