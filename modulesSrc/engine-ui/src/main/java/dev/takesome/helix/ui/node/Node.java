package dev.takesome.helix.ui.node;

import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.render.UiRenderContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Base retained-mode UI node.
 *
 * <p>Bounds are local to the parent. Rendering and hit testing use absolute
 * bounds resolved by walking the parent chain.</p>
 */
public abstract class Node {
    private final NodeChildren children = new NodeChildren();
    private final NodeTraversal traversal = new NodeTraversal();
    private final NodeInputRouter inputRouter = new NodeInputRouter();
    private final NodeGeometry geometry = new NodeGeometry();
    private final ArrayList<NodeRuntimeBinding> runtimeBindings = new ArrayList<>();

    private Node parent;
    private boolean visible = true;
    private boolean enabled = true;
    private float opacity = 1f;
    private int zOrder;
    private boolean clipChildren;
    private boolean attached;
    private boolean dirty = true;

    public final void add(final Node child) {
        children.add(this, traversal, child);
    }

    public final void remove(final Node child) {
        children.remove(this, traversal, child);
    }

    public final void clear() {
        children.clear(this, traversal);
    }

    public final void bringChildToFront(final Node child) {
        children.bringToFront(this, traversal, child);
    }

    public final void sendChildToBack(final Node child) {
        children.sendToBack(this, traversal, child);
    }

    public final void moveChildBefore(final Node child, final Node sibling) {
        children.moveBefore(this, traversal, child, sibling);
    }

    public final void moveChildAfter(final Node child, final Node sibling) {
        children.moveAfter(this, traversal, child, sibling);
    }

    public final void sortChildrenByZIndex() {
        children.sortByZIndex(this, traversal);
    }

    public final void addRuntimeBinding(NodeRuntimeBinding binding) {
        if (binding == null || runtimeBindings.contains(binding)) return;
        runtimeBindings.add(binding);
    }

    public final void removeRuntimeBinding(NodeRuntimeBinding binding) {
        runtimeBindings.remove(binding);
    }

    public final void clearRuntimeBindings() {
        runtimeBindings.clear();
    }

    public final void attach() {
        if (attached) return;
        attached = true;
        onAttach();
        for (int i = 0; i < children.size(); i++) children.get(i).attach();
    }

    public final void detach() {
        if (!attached) return;
        for (int i = children.size() - 1; i >= 0; i--) children.get(i).detach();
        onDetach();
        attached = false;
    }

    public final void update(float dt) {
        if (!enabled) return;
        traversal.update(this, children, dt);
    }

    final void updateRuntimeBindingsInternal(float dt) {
        if (runtimeBindings.isEmpty()) return;
        for (int i = 0; i < runtimeBindings.size(); i++) runtimeBindings.get(i).update(this, dt);
    }

    public final void render(UiRenderContext ctx) {
        if (!visible) return;
        traversal.render(this, children, ctx);
    }

    public final boolean handleInput(UiInputEvent event) {
        return inputRouter.handle(this, children, traversal, event);
    }

    public final void setBounds(float x, float y, float width, float height) {
        geometry.setBounds(this, x, y, width, height);
    }

    public final void setPosition(float x, float y) {
        setBounds(x, y, geometry.width(), geometry.height());
    }

    public final void setSize(float width, float height) {
        setBounds(geometry.x(), geometry.y(), width, height);
    }

    public final UiRect bounds() {
        return geometry.bounds();
    }

    public final UiRect absoluteBounds() {
        return geometry.absoluteBounds(this);
    }

    public final boolean containsAbsolute(float px, float py) {
        return geometry.containsAbsolute(this, px, py);
    }

    public final float absoluteX() {
        return geometry.absoluteX(this);
    }

    public final float absoluteY() {
        return geometry.absoluteY(this);
    }

    public final Node parent() {
        return parent;
    }

    public final List<Node> children() {
        return children.readonly();
    }

    public final int zIndex() {
        return zOrder;
    }

    public final void setZIndex(int zIndex) {
        if (zOrder == zIndex) return;
        zOrder = zIndex;
        markDirty();
        if (parent != null) parent.sortChildrenByZIndex();
    }

    public final boolean isVisible() {
        return visible;
    }

    public final void setVisible(boolean visible) {
        if (this.visible == visible) return;
        this.visible = visible;
        markDirty();
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public final float opacity() {
        return opacity;
    }

    public final void setOpacity(float opacity) {
        float next = Float.isFinite(opacity) ? Math.max(0f, Math.min(1f, opacity)) : 1f;
        if (this.opacity == next) return;
        this.opacity = next;
        markDirty();
    }

    public final boolean clipsChildren() {
        return clipChildren;
    }

    public final void setClipChildren(boolean clipChildren) {
        if (this.clipChildren == clipChildren) return;
        this.clipChildren = clipChildren;
        markDirty();
    }


    public final boolean isDirty() {
        return dirty;
    }

    public final void markDirty() {
        dirty = true;
        if (parent != null) parent.markDirty();
    }

    final Node parentInternal() {
        return parent;
    }

    final void setParentInternal(Node parent) {
        this.parent = parent;
    }

    final boolean attachedInternal() {
        return attached;
    }

    final void clearDirtyInternal() {
        dirty = false;
    }

    final float localXInternal() {
        return geometry.x();
    }

    final float localYInternal() {
        return geometry.y();
    }

    protected UiRect debugContentBounds(UiRenderContext ctx) {
        return absoluteBounds();
    }

    protected String debugLabel() {
        return getClass().getSimpleName();
    }

    private int hierarchyDepth() {
        int depth = 0;
        Node cursor = parent;
        while (cursor != null) {
            depth++;
            cursor = cursor.parent;
        }
        return depth;
    }

    protected void onParentChanged(Node nextParent) {
    }

    protected void onAttach() {
    }

    protected void onDetach() {
    }

    protected void onBoundsChanged() {
    }

    protected void onUpdate(float dt) {
    }

    protected void onRender(UiRenderContext ctx) {
    }

    protected void onRenderOverlay(UiRenderContext ctx) {
    }

    protected boolean onInputCapture(UiInputEvent event) {
        return false;
    }

    protected boolean onInput(UiInputEvent event) {
        return false;
    }
}
