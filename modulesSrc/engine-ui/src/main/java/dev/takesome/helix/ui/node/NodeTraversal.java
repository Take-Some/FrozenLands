package dev.takesome.helix.ui.node;

import dev.takesome.helix.ui.render.UiRenderContext;

final class NodeTraversal {
    private int depth;

    boolean inTraversal() {
        return depth > 0;
    }

    void update(Node owner, NodeChildren children, float dt) {
        begin();
        try {
            owner.updateRuntimeBindingsInternal(dt);
            owner.onUpdate(dt);
            for (int i = 0; i < children.size(); i++) children.get(i).update(dt);
        } finally {
            end(children);
        }
    }

    void render(Node owner, NodeChildren children, UiRenderContext ctx) {
        begin();
        try {
            boolean opacityPushed = owner.opacity() < 0.999f && ctx.pushOpacity(owner.opacity());
            try {
                owner.onRender(ctx);
                boolean clipped = owner.clipsChildren() && ctx.pushClip(owner.absoluteBounds());
                try {
                    for (int i = 0; i < children.size(); i++) children.get(i).render(ctx);
                } finally {
                    if (clipped) ctx.popClip();
                }
                owner.onRenderOverlay(ctx);
            } finally {
                if (opacityPushed) ctx.popOpacity();
            }
            owner.clearDirtyInternal();
        } finally {
            end(children);
        }
    }

    void begin() {
        depth++;
    }

    void end(NodeChildren children) {
        depth--;
        if (depth == 0) children.flushDeferred();
    }
}
