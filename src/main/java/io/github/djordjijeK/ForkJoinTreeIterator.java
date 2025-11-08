package io.github.djordjijeK;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;


class ForkJoinTreeIterator<T> implements Iterator<T> {
    private final boolean ascending;
    private final Stack<TreeNode<T>> stack;


    ForkJoinTreeIterator(TreeNode<T> root) {
        this.stack = new Stack<>();
        this.ascending = true;

        pushPath(root);
    }


    ForkJoinTreeIterator(TreeNode<T> root, boolean ascending) {
        this.stack = new Stack<>();
        this.ascending = ascending;

        pushPath(root);
    }


    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        TreeNode<T> node = stack.pop();
        T result = node.element;

        pushPath(ascending ? node.right : node.left);

        return result;
    }


    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }


    private void pushPath(TreeNode<T> node) {
        while (node != null) {
            stack.push(node);
            node = ascending ? node.left : node.right;
        }
    }
}
