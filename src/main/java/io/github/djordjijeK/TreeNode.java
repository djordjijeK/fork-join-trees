package io.github.djordjijeK;


class TreeNode<T> {
    T element;
    TreeNode<T> left;
    TreeNode<T> right;
    int height;
    int size;


    TreeNode(T element) {
        this.element = element;
        this.left = null;
        this.right = null;
        this.height = 1;
        this.size = 1;
    }


    TreeNode(T element, TreeNode<T> left, TreeNode<T> right) {
        this.element = element;
        this.left = left;
        this.right = right;
        this.update();
    }


    void update() {
        this.height = Math.max(left != null ? left.height : 0, right != null ? right.height : 0) + 1;
        this.size = 1 + (this.left != null ? this.left.size : 0) + (this.right != null ? this.right.size : 0);
    }
}
