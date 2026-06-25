package dev.takesome.helix.ui.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class NodeChildren {
    private final ArrayList<Node> children = new ArrayList<>();
    private final List<Node> readonlyChildren = Collections.unmodifiableList(children);
    private final ArrayList<Runnable> deferred = new ArrayList<>();

    void add(Node owner, NodeTraversal traversal, Node child) {
        requireChild(owner, child);
        mutate(traversal, () -> addNow(owner, child));
    }

    void remove(Node owner, NodeTraversal traversal, Node child) {
        if (child == null) throw new IllegalArgumentException("child must not be null");
        mutate(traversal, () -> removeNow(owner, child));
    }

    void clear(Node owner, NodeTraversal traversal) {
        mutate(traversal, () -> clearNow(owner));
    }

    void bringToFront(Node owner, NodeTraversal traversal, Node child) {
        if (child == null) throw new IllegalArgumentException("child must not be null");
        mutate(traversal, () -> moveToIndexNow(owner, child, children.size() - 1));
    }

    void sendToBack(Node owner, NodeTraversal traversal, Node child) {
        if (child == null) throw new IllegalArgumentException("child must not be null");
        mutate(traversal, () -> moveToIndexNow(owner, child, 0));
    }

    void moveBefore(Node owner, NodeTraversal traversal, Node child, Node sibling) {
        if (child == null || sibling == null) throw new IllegalArgumentException("child and sibling must not be null");
        mutate(traversal, () -> moveRelativeNow(owner, child, sibling, false));
    }

    void moveAfter(Node owner, NodeTraversal traversal, Node child, Node sibling) {
        if (child == null || sibling == null) throw new IllegalArgumentException("child and sibling must not be null");
        mutate(traversal, () -> moveRelativeNow(owner, child, sibling, true));
    }

    void sortByZIndex(Node owner, NodeTraversal traversal) {
        mutate(traversal, () -> sortByZIndexNow(owner));
    }

    List<Node> readonly() {
        return readonlyChildren;
    }

    int size() {
        return children.size();
    }

    Node get(int index) {
        return children.get(index);
    }

    void flushDeferred() {
        while (!deferred.isEmpty()) {
            ArrayList<Runnable> mutations = new ArrayList<>(deferred);
            deferred.clear();
            for (int i = 0; i < mutations.size(); i++) mutations.get(i).run();
        }
    }

    private void mutate(NodeTraversal traversal, Runnable mutation) {
        if (traversal.inTraversal()) {
            deferred.add(mutation);
            return;
        }
        mutation.run();
    }

    private void addNow(Node owner, Node child) {
        child.setParentInternal(owner);
        children.add(child);
        if (owner.attachedInternal()) child.attach();
        owner.markDirty();
        child.onParentChanged(owner);
    }

    private void removeNow(Node owner, Node child) {
        if (!children.remove(child)) return;
        if (child.attachedInternal()) child.detach();
        child.setParentInternal(null);
        child.onParentChanged(null);
        owner.markDirty();
    }

    private void clearNow(Node owner) {
        ArrayList<Node> snapshot = new ArrayList<>(children);
        for (int i = 0; i < snapshot.size(); i++) removeNow(owner, snapshot.get(i));
    }

    private void moveRelativeNow(Node owner, Node child, Node sibling, boolean after) {
        int childIndex = children.indexOf(child);
        int siblingIndex = children.indexOf(sibling);
        if (childIndex < 0 || siblingIndex < 0) throw new IllegalArgumentException("child and sibling must belong to the same parent");
        if (child == sibling) return;
        children.remove(childIndex);
        if (childIndex < siblingIndex) siblingIndex--;
        int target = after ? siblingIndex + 1 : siblingIndex;
        children.add(Math.max(0, Math.min(target, children.size())), child);
        owner.markDirty();
    }

    private void moveToIndexNow(Node owner, Node child, int targetIndex) {
        int childIndex = children.indexOf(child);
        if (childIndex < 0) throw new IllegalArgumentException("child must belong to this parent");
        children.remove(childIndex);
        children.add(Math.max(0, Math.min(targetIndex, children.size())), child);
        owner.markDirty();
    }

    private void sortByZIndexNow(Node owner) {
        if (children.size() < 2) return;
        ArrayList<Node> before = new ArrayList<>(children);
        children.sort(java.util.Comparator.comparingInt(Node::zIndex));
        if (!children.equals(before)) owner.markDirty();
    }

    private void requireChild(Node owner, Node child) {
        if (child == null) throw new IllegalArgumentException("child must not be null");
        if (child == owner) throw new IllegalArgumentException("node cannot be parented to itself");
        if (child.parentInternal() != null) throw new IllegalStateException("child already has parent");
        if (isAncestorOf(child, owner)) throw new IllegalArgumentException("node cycle is not allowed");
    }

    private boolean isAncestorOf(Node candidateAncestor, Node node) {
        Node cursor = node;
        while (cursor != null) {
            if (cursor == candidateAncestor) return true;
            cursor = cursor.parentInternal();
        }
        return false;
    }
}
