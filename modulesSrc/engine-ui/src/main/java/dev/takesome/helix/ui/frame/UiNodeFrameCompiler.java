package dev.takesome.helix.ui.frame;

import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.uiComponents.button.UiButtonNode;
import dev.takesome.helix.ui.uiComponents.checkbox.UiCheckboxNode;
import dev.takesome.helix.ui.components.UiElementNode;
import dev.takesome.helix.ui.uiComponents.image.UiImageNode;
import dev.takesome.helix.ui.uiComponents.input.UiInputNode;
import dev.takesome.helix.ui.uiComponents.label.UiLabelNode;
import dev.takesome.helix.ui.uiComponents.panel.UiPanelNode;
import dev.takesome.helix.ui.node.ContainerNode;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.node.SceneNode;
import dev.takesome.helix.ui.render.UiRenderContext;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/** Lowers retained UI runtime nodes into the canonical UiFrame IR. */
public final class UiNodeFrameCompiler {
    public UiFrame compile(Node root, UiRenderContext renderContext) {
        if (root == null) return UiFrame.empty();
        UiRect bounds = root.absoluteBounds();
        return compile(root, bounds.w, bounds.h, renderContext);
    }

    public UiFrame compile(Node root, float width, float height, UiRenderContext renderContext) {
        if (root == null) return UiFrame.empty();
        ArrayList<UiFrameNode> nodes = new ArrayList<>();
        IdentityHashMap<Node, Integer> ids = new IdentityHashMap<>();
        collect(root, UiFrameNode.NO_PARENT, 0, nodes, ids);

        UiFrameRecorder recorder = new UiFrameRecorder(renderContext, UiFrameRenderCapabilities.from(renderContext));
        root.render(recorder);
        return new UiFrame(width, height, nodes, recorder.commands());
    }

    private void collect(
            Node node,
            int parentId,
            int depth,
            List<UiFrameNode> out,
            IdentityHashMap<Node, Integer> ids
    ) {
        int id = ids.size();
        ids.put(node, id);
        out.add(new UiFrameNode(
                id,
                parentId,
                kind(node, parentId),
                node.absoluteBounds(),
                node.isVisible(),
                node.isEnabled(),
                depth,
                UiFrameStyle.empty(),
                List.of(),
                List.of()
        ));
        for (Node child : node.children()) {
            collect(child, id, depth + 1, out, ids);
        }
    }

    private UiFrameNodeKind kind(Node node, int parentId) {
        if (node instanceof SceneNode) return UiFrameNodeKind.ROOT;
        if (node instanceof UiButtonNode) return UiFrameNodeKind.BUTTON;
        if (node instanceof UiCheckboxNode) return UiFrameNodeKind.CHECKBOX;
        if (node instanceof UiInputNode) return UiFrameNodeKind.INPUT;
        if (node instanceof UiLabelNode) return UiFrameNodeKind.TEXT;
        if (node instanceof UiImageNode) return UiFrameNodeKind.IMAGE;
        if (node instanceof UiPanelNode) return UiFrameNodeKind.PANEL;
        if (node instanceof UiElementNode) return UiFrameNodeKind.ELEMENT;
        if (node instanceof ContainerNode) return parentId == UiFrameNode.NO_PARENT ? UiFrameNodeKind.ROOT : UiFrameNodeKind.CONTAINER;
        return UiFrameNodeKind.CUSTOM;
    }
}
