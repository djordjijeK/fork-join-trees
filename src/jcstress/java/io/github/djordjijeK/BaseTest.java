package io.github.djordjijeK;

public abstract class BaseTest {

    static <T extends Comparable<T>> InvariantCheckResult checkInvariants(TreeNode<T> root) {
        if (root == null) {
            return new InvariantCheckResult(true, 0, null, null);
        }

        InvariantCheckResult leftResult = checkInvariants(root.left);
        if (!leftResult.isValid) {
            return INVALID_RESULT;
        }

        InvariantCheckResult rightResult = checkInvariants(root.right);
        if (!rightResult.isValid) {
            return INVALID_RESULT;
        }

        // Check BST property
        if (leftResult.max != null && ((T) leftResult.max).compareTo(root.element) >= 0) {
            return INVALID_RESULT;
        }
        if (rightResult.min != null && root.element.compareTo((T) rightResult.min) >= 0) {
            return INVALID_RESULT;
        }

        // Check height calculation
        int computedHeight = 1 + Math.max(leftResult.height, rightResult.height);
        if (root.height != computedHeight) {
            return INVALID_RESULT;
        }

        // Check AVL property
        if (Math.abs(leftResult.height - rightResult.height) > 1) {
            return INVALID_RESULT;
        }

        Comparable<?> newMin = leftResult.min != null ? leftResult.min : root.element;
        Comparable<?> newMax = rightResult.max != null ? rightResult.max : root.element;

        return new InvariantCheckResult(true, computedHeight, newMin, newMax);
    }


    private static final InvariantCheckResult INVALID_RESULT = new InvariantCheckResult(false, 0, null, null);


    static final class InvariantCheckResult {
        final boolean isValid;
        final int height;
        final Comparable<?> min;
        final Comparable<?> max;

        InvariantCheckResult(boolean isValid, int height, Comparable<?> min, Comparable<?> max) {
            this.isValid = isValid;
            this.height = height;
            this.min = min;
            this.max = max;
        }
    }
}
