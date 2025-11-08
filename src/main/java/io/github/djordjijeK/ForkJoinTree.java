package io.github.djordjijeK;

import java.util.Iterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicReference;


public class ForkJoinTree<T extends Comparable<T>> implements Iterable<T> {
    private final ForkJoinPool forkJoinPool;
    final AtomicReference<TreeNode<T>> root;


    public ForkJoinTree() {
        this.root = new AtomicReference<>();
        this.forkJoinPool = ForkJoinPool.commonPool();
    }


    private ForkJoinTree(TreeNode<T> root) {
        this.root = new AtomicReference<>(root);
        this.forkJoinPool = ForkJoinPool.commonPool();
    }


    public void insert(T element) {
        TreeNode<T> oldRoot;
        TreeNode<T> newRoot;
        do {
            oldRoot = root.get();
            newRoot = insert(oldRoot, element);
        } while (!root.compareAndSet(oldRoot, newRoot));
    }


    public void delete(T element) {
        TreeNode<T> oldRoot;
        TreeNode<T> newRoot;
        do {
            oldRoot = root.get();
            newRoot = delete(oldRoot, element);
        } while (!root.compareAndSet(oldRoot, newRoot));
    }


    public int size() {
        if (this.root.get() == null) {
            return 0;
        }

        return this.root.get().size;
    }


    public ForkJoinTree<T> lowerThan(T element) {
        return new ForkJoinTree<>(lowerThan(this.root.get(), element));
    }


    public ForkJoinTree<T> greaterThan(T element) {
        return new ForkJoinTree<>(greaterThan(this.root.get(), element));
    }


    public ForkJoinTree<T> range(T low, T high) {
        return new ForkJoinTree<>(range(this.root.get(), low, high));
    }


    public boolean contains(T element) {
        return contains(this.root.get(), element);
    }


    public Split<T> split(T element) {
        InternalSplit<T> split = split(this.root.get(), element);

        return new Split<>(
                split.element,
                new ForkJoinTree<>(split.left),
                new ForkJoinTree<>(split.right)
        );
    }


    public ForkJoinTree<T> union(ForkJoinTree<T> other) {
        return new ForkJoinTree<>(union(this.root.get(), other.root.get()));
    }


    public ForkJoinTree<T> intersection(ForkJoinTree<T> other) {
        return new ForkJoinTree<>(intersection(this.root.get(), other.root.get()));
    }


    public ForkJoinTree<T> difference(ForkJoinTree<T> other) {
        return new ForkJoinTree<>(difference(this.root.get(), other.root.get()));
    }


    @Override
    public Iterator<T> iterator() {
        return new ForkJoinTreeIterator<T>(this.root.get());
    }


    public Iterator<T> iteratorDescending() {
        return new ForkJoinTreeIterator<T>(this.root.get(), false);
    }


    private TreeNode<T> insert(TreeNode<T> root, T element) {
        if (root == null) {
            return new TreeNode<>(element);
        }

        int compareResult = element.compareTo(root.element);

        if (compareResult < 0) {
            TreeNode<T> left = insert(root.left, element);
            TreeNode<T> right = root.right;
            TreeNode<T> center = new TreeNode<>(root.element);

            return join(center, left, right);
        } else if (compareResult > 0) {
            TreeNode<T> left = root.left;
            TreeNode<T> right = insert(root.right, element);
            TreeNode<T> center = new TreeNode<>(root.element);

            return join(center, left, right);
        } else {
            TreeNode<T> left = root.left;
            TreeNode<T> right = root.right;
            TreeNode<T> center = new TreeNode<>(element);

            return join(center, left, right);
        }
    }


    private InternalSplit<T> split(TreeNode<T> root, T element) {
        if (root == null) {
            return new InternalSplit<>(null, null, null);
        }

        int compareResult = element.compareTo(root.element);

        if (compareResult < 0) {
            InternalSplit<T> InternalSplit = split(root.left, element);

            TreeNode<T> right = join(
                    new TreeNode<>(root.element),
                    InternalSplit.right(),
                    root.right
            );

            return new InternalSplit<>(InternalSplit.element(), InternalSplit.left(), right);
        } else if (compareResult > 0) {
            InternalSplit<T> InternalSplit = split(root.right, element);

            TreeNode<T> left = join(
                    new TreeNode<>(root.element),
                    root.left,
                    InternalSplit.left()
            );

            return new InternalSplit<>(InternalSplit.element(), left, InternalSplit.right());
        } else {
            return new InternalSplit<>(element, root.left, root.right);
        }
    }


    private TreeNode<T> union(TreeNode<T> left, TreeNode<T> right) {
        ForkJoinTask<TreeNode<T>> unionTask = this.forkJoinPool.submit(new UnionTask(left, right));
        return unionTask.join();
    }


    private TreeNode<T> intersection(TreeNode<T> left, TreeNode<T> right) {
        ForkJoinTask<TreeNode<T>> intersectionTask = this.forkJoinPool.submit(new IntersectionTask(left, right));
        return intersectionTask.join();
    }


    private TreeNode<T> difference(TreeNode<T> left, TreeNode<T> right) {
        ForkJoinTask<TreeNode<T>> differenceTask = this.forkJoinPool.submit(new DifferenceTask(left, right));
        return differenceTask.join();
    }


    private boolean contains(TreeNode<T> root, T element) {
        while (root != null) {
            int compareResult = element.compareTo(root.element);

            if (compareResult < 0) {
                root = root.left;
            } else if (compareResult > 0) {
                root = root.right;
            } else {
                return true;
            }
        }

        return false;
    }


    private TreeNode<T> delete(TreeNode<T> root, T element) {
        if (root == null) {
            return null;
        }

        int compareResult = element.compareTo(root.element);

        if (compareResult < 0) {
            TreeNode<T> left = delete(root.left, element);
            TreeNode<T> right = root.right;
            TreeNode<T> center = new TreeNode<>(root.element);

            return join(center, left, right);
        } else if (compareResult > 0) {
            TreeNode<T> left = root.left;
            TreeNode<T> right = delete(root.right, element);
            TreeNode<T> center = new TreeNode<>(root.element);

            return join(center, left, right);
        } else {
            return joinLeftRight(root.left, root.right);
        }
    }


    private TreeNode<T> lowerThan(TreeNode<T> root, T element) {
        if (root == null) {
            return null;
        }

        if (element.compareTo(root.element) < 0) {
            return lowerThan(root.left, element);
        }

        TreeNode<T> center = new TreeNode<>(root.element);
        return join(center, root.left, lowerThan(root.right, element));
    }


    private TreeNode<T> greaterThan(TreeNode<T> root, T element) {
        if (root == null) {
            return null;
        }

        if (element.compareTo(root.element) > 0) {
            return greaterThan(root.right, element);
        }

        TreeNode<T> center = new TreeNode<>(root.element);
        return join(center, greaterThan(root.left, element), root.right);
    }


    private TreeNode<T> range(TreeNode<T> root, T low, T high) {
        if (root == null) {
            return null;
        }

        int lowComparisonResult = low.compareTo(root.element);
        int highComparisonResult = root.element.compareTo(high);

        if (lowComparisonResult > 0) {
            // Current node < low: range is entirely in right subtree
            return range(root.right, low, high);
        }

        if (highComparisonResult > 0) {
            // Current node > high: range is entirely in left subtree
            return range(root.left, low, high);
        }

        // Current node is in range [low, high]
        // Recursively get elements from left (>= low) and right (<= high)
        ForkJoinTask<TreeNode<T>> leftRangeTask = forkJoinPool.submit(() -> greaterThan(root.left, low));
        ForkJoinTask<TreeNode<T>> rightRangeTask = forkJoinPool.submit(() -> lowerThan(root.right, high));

        return join(new TreeNode<>(root.element), leftRangeTask.join(), rightRangeTask.join());
    }


    // INVARIANT: exclusive access to center
    private TreeNode<T> join(TreeNode<T> center, TreeNode<T> left, TreeNode<T> right) {
        switch (getHeavierSide(left, right)) {
            case LEFT -> {
                return rightJoin(center, left, right);
            }
            case RIGHT -> {
                return leftJoin(center, left, right);
            }
        }

        return balancedJoin(center, left, right);
    }


    // INVARIANT: all elements in left < all elements in right
    private TreeNode<T> joinLeftRight(TreeNode<T> left, TreeNode<T> right) {
        if (left == null) {
            return right;
        }

        if (right == null) {
            return left;
        }

        if (left.height > right.height) {
            TreeNode<T> newLeft = left.left;
            TreeNode<T> newRight = joinLeftRight(left.right, right);
            TreeNode<T> center = new TreeNode<>(left.element, left.left, left.right);

            return join(center, newLeft, newRight);
        } else {
            TreeNode<T> newLeft = joinLeftRight(left, right.left);
            TreeNode<T> newRight = right.right;
            TreeNode<T> center = new TreeNode<>(right.element, right.left, right.right);

            return join(center, newLeft, newRight);
        }
    }


    private TreeNode<T> rightJoin(TreeNode<T> center, TreeNode<T> left, TreeNode<T> right) {
        if (getHeavierSide(left, right) != TreeSide.LEFT) {
            return balancedJoin(center, left, right);
        }

        // INVARIANT: ensure exclusive access to left and left.right nodes by making a copy of them
        left = new TreeNode<>(left.element, left.left, left.right);
        left.right = rightJoin(center, left.right, right);

        if (getHeavierSide(left.left, left.right) == TreeSide.RIGHT) {
            if (singleRotationRequired(left.right, TreeSide.RIGHT)) {
                left = rotateLeft(left);        // INVARIANT: exclusive access to root and root.right
            } else {
                left = rotateRightLeft(left);   // INVARIANT: exclusive access to root and root.right
            }
        } else {
            left.update();
        }

        return left;
    }


    private TreeNode<T> leftJoin(TreeNode<T> center, TreeNode<T> left, TreeNode<T> right) {
        if (getHeavierSide(left, right) != TreeSide.RIGHT) {
            return balancedJoin(center, left, right);
        }

        // INVARIANT: ensure exclusive access to right and right.left nodes by making a copy of them
        right = new TreeNode<>(right.element, right.left, right.right);
        right.left = leftJoin(center, left, right.left);

        if (getHeavierSide(right.left, right.right) == TreeSide.LEFT) {
            if (singleRotationRequired(right.left, TreeSide.LEFT)) {
                right = rotateRight(right);         // INVARIANT: exclusive access to root and root.left
            } else {
                right = rotateLeftRight(right);     // INVARIANT: exclusive access to root and root.left
            }
        } else {
            right.update();
        }

        return right;
    }


    // INVARIANT: exclusive access to center
    private TreeNode<T> balancedJoin(TreeNode<T> center, TreeNode<T> left, TreeNode<T> right) {
        center.left = left;
        center.right = right;
        center.update();

        return center;
    }


    // INVARIANT: exclusive access to root and root.rightChild
    private TreeNode<T> rotateRightLeft(TreeNode<T> root) {
        TreeNode<T> currentRootRight = root.right;

        // INVARIANT: exclusive access to currentRoot.rightChild.leftChild
        TreeNode<T> currentRootRightLeft = new TreeNode<>(currentRootRight.left.element, currentRootRight.left.left, currentRootRight.left.right);

        currentRootRight.left = currentRootRightLeft.right;
        currentRootRight.update();

        currentRootRightLeft.right = currentRootRight;
        root.right = currentRootRightLeft;

        // INVARIANT: exclusive access to root and root.rightChild
        return rotateLeft(root);
    }


    // INVARIANT: exclusive access to root and root.leftChild
    private TreeNode<T> rotateLeftRight(TreeNode<T> root) {
        TreeNode<T> currentRootLeft = root.left;

        // INVARIANT: exclusive access to root.leftChild.rightChild
        TreeNode<T> currentRootLeftRight = new TreeNode<>(currentRootLeft.right.element, currentRootLeft.right.left, currentRootLeft.right.right);

        currentRootLeft.right = currentRootLeftRight.left;
        currentRootLeft.update();

        currentRootLeftRight.left = currentRootLeft;
        root.left = currentRootLeftRight;

        // INVARIANT: exclusive access to root and root.leftChild
        return rotateRight(root);
    }


    // INVARIANT: exclusive access to root and root.rightChild
    private TreeNode<T> rotateLeft(TreeNode<T> root) {
        TreeNode<T> newRoot = root.right;
        TreeNode<T> newRootLeftChild = newRoot.left;

        newRoot.left = root;
        root.right = newRootLeftChild;

        newRoot.left.update();
        newRoot.update();

        return newRoot;
    }


    // INVARIANT: exclusive access to root and root.leftChild
    private TreeNode<T> rotateRight(TreeNode<T> root) {
        TreeNode<T> newRoot = root.left;
        TreeNode<T> newRootRightChild = newRoot.right;

        newRoot.right = root;
        root.left = newRootRightChild;

        newRoot.right.update();
        newRoot.update();

        return newRoot;
    }


    private TreeSide getHeavierSide(TreeNode<T> left, TreeNode<T> right) {
        int leftSideHeight = left != null ? left.height : 0;
        int rightSideHeight = right != null ? right.height : 0;

        int difference = leftSideHeight - rightSideHeight;

        if (difference > 1) {
            return TreeSide.LEFT;
        }

        if (difference < -1) {
            return TreeSide.RIGHT;
        }

        return TreeSide.NONE;
    }


    private boolean singleRotationRequired(TreeNode<T> root, TreeSide side) {
        assert side != TreeSide.NONE;

        int leftChildHeight = root.left != null ? root.left.height : 0;
        int rightChildHeight = root.right != null ? root.right.height : 0;

        if (side == TreeSide.RIGHT) {
            return rightChildHeight - leftChildHeight > 0;
        }

        return leftChildHeight - rightChildHeight > 0;
    }


    private final class UnionTask extends RecursiveTask<TreeNode<T>> {
        private final TreeNode<T> left;
        private final TreeNode<T> right;


        UnionTask(TreeNode<T> left, TreeNode<T> right) {
            this.left = left;
            this.right = right;
        }


        @Override
        protected TreeNode<T> compute() {
            if (left == null) {
                return right;
            }

            if (right == null) {
                return left;
            }

            TreeNode<T> center = new TreeNode<>(right.element);
            InternalSplit<T> split = split(left, right.element);

            ForkJoinTask<TreeNode<T>> leftTask = new UnionTask(split.left(), right.left).fork();
            ForkJoinTask<TreeNode<T>> rightTask = new UnionTask(split.right(), right.right).fork();

            return ForkJoinTree.this.join(center, leftTask.join(), rightTask.join());
        }
    }


    private final class IntersectionTask extends RecursiveTask<TreeNode<T>> {
        private final TreeNode<T> left;
        private final TreeNode<T> right;


        IntersectionTask(TreeNode<T> left, TreeNode<T> right) {
            this.left = left;
            this.right = right;
        }


        @Override
        protected TreeNode<T> compute() {
            if (left == null) {
                return null;
            }

            if (right == null) {
                return null;
            }

            InternalSplit<T> split = split(left, right.element);

            ForkJoinTask<TreeNode<T>> leftTask = new IntersectionTask(split.left(), right.left).fork();
            ForkJoinTask<TreeNode<T>> rightTask = new IntersectionTask(split.right(), right.right).fork();

            if (split.element != null) {
                return ForkJoinTree.this.join(new TreeNode<T>(split.element), leftTask.join(), rightTask.join());
            } else {
                return ForkJoinTree.this.joinLeftRight(leftTask.join(), rightTask.join());
            }
        }
    }


    private final class DifferenceTask extends RecursiveTask<TreeNode<T>> {
        private final TreeNode<T> left;
        private final TreeNode<T> right;


        DifferenceTask(TreeNode<T> left, TreeNode<T> right) {
            this.left = left;
            this.right = right;
        }


        @Override
        protected TreeNode<T> compute() {
            if (left == null) {
                return null;
            }

            if (right == null) {
                return left;
            }

            InternalSplit<T> split = split(right, left.element);

            ForkJoinTask<TreeNode<T>> leftTask = new DifferenceTask(left.left, split.left).fork();
            ForkJoinTask<TreeNode<T>> rightTask = new DifferenceTask(left.right, split.right).fork();

            if (split.element != null) {
                return ForkJoinTree.this.joinLeftRight(leftTask.join(), rightTask.join());
            } else {
                return ForkJoinTree.this.join(new TreeNode<>(left.element), leftTask.join(), rightTask.join());
            }
        }
    }


    private record InternalSplit<T>(T element, TreeNode<T> left, TreeNode<T> right) {
    }
}
