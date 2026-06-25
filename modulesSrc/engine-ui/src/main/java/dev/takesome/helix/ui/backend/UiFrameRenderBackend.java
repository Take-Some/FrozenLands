package dev.takesome.helix.ui.backend;

import dev.takesome.helix.ui.frame.UiFrame;
import dev.takesome.helix.ui.frame.UiFrameCommand;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Default backend that replays UiFrame commands into an existing UiRenderContext adapter. */
public final class UiFrameRenderBackend implements UiRenderBackend {
    @Override
    public void render(UiFrame frame, UiRenderContext target) {
        if (frame == null || target == null) return;
        for (UiFrameCommand command : frame.commands()) render(command, target);
    }

    private void render(UiFrameCommand command, UiRenderContext target) {
        if (command == null || command.kind() == null) return;
        switch (command.kind()) {
            case FILL:
                target.fill(command.rect(), command.color());
                break;
            case STROKE:
                target.stroke(command.rect(), command.color(), command.strokeWidth());
                break;
            case BOX_SHADOW:
                target.boxShadow(command.rect(), command.boxShadow());
                break;
            case TEXT:
                target.text(command.text(), command.rect(), command.scale(), command.color(), command.align(), command.fontId());
                break;
            case BUTTON_TEXT:
                target.buttonText(command.text(), command.rect(), command.scale(), command.color(), command.align(), command.fontId());
                break;
            case ICON:
                target.icon(command.icon(), command.rect(), command.scale(), command.color(), command.align());
                break;
            case IMAGE:
                target.image(command.imageSource(), command.rect(), command.opacity());
                break;
            case ELEMENT:
                target.drawElement(command.element(), command.rect(), command.tint());
                break;
            case TRACE_NODE:
                target.traceNode(command.rect(), command.rect(), command.depth(), command.label());
                break;
            default:
                break;
        }
    }
}
