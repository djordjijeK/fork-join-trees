package io.github.djordjijeK;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ForkJoinTreeTests {
    @Test
    void insertMaintainsTreeInvariants() {
        ForkJoinTree<String> forkJoinTree = new ForkJoinTree<>();

        for (String element : generateRandomStrings(1000)) {
            forkJoinTree.insert(element);
            assertTrue(areInvariantsSatisfied(forkJoinTree.root.get()), "Tree invariants violated after inserting: " + element);
        }

        assertEquals(1000, forkJoinTree.size());
    }


    @Test
    void insertHandlesDuplicatesCorrectly() {
        ForkJoinTree<Integer> forkJoinTree = new ForkJoinTree<>();
        TreeSet<Integer> oracle = new TreeSet<>();

        IntStream.range(0, 1000).forEach(i -> {
            int randomValue = ThreadLocalRandom.current().nextInt(0, 100);
            forkJoinTree.insert(randomValue);
            oracle.add(randomValue);
        });

        assertTrue(areInvariantsSatisfied(forkJoinTree.root.get()));
        assertEquals(oracle.size(), forkJoinTree.size());
        assertTreeEqualsSet(forkJoinTree, oracle);
    }


    @Test
    void deleteMaintainsTreeInvariants() {
        ForkJoinTree<String> forkJoinTree = createRandomStringTree(1000);

        for (String element : forkJoinTree) {
            forkJoinTree.delete(element);
            assertTrue(areInvariantsSatisfied(forkJoinTree.root.get()), "Tree invariants violated after deleting: " + element);
        }

        assertEquals(0, forkJoinTree.size());
    }


    @Test
    void containsReturnsCorrectResults() {
        ForkJoinTree<Integer> forkJoinTree = new ForkJoinTree<>();
        assertFalse(forkJoinTree.contains(42));

        List<Integer> elements = List.of(50, 25, 75, 10, 30, 60, 90);

        elements.forEach(forkJoinTree::insert);
        elements.forEach(element -> assertTrue(forkJoinTree.contains(element), "Should contain " + element));

        // Test non-existing elements
        List<Integer> nonExistentElements = List.of(5, 35, 100);
        nonExistentElements.forEach(element -> assertFalse(forkJoinTree.contains(element), "Should not contain " + element));

        // Test after deletion
        forkJoinTree.delete(25);
        assertFalse(forkJoinTree.contains(25), "Should not contain deleted element");
        assertTrue(forkJoinTree.contains(50), "Should still contain other elements");
    }


    @Test
    void sizeTracksOperationsCorrectly() {
        ForkJoinTree<Integer> forkJoinTree = new ForkJoinTree<>();
        assertEquals(0, forkJoinTree.size());

        forkJoinTree.insert(50);
        assertEquals(1, forkJoinTree.size());

        forkJoinTree.insert(25);
        forkJoinTree.insert(75);
        assertEquals(3, forkJoinTree.size());

        forkJoinTree.insert(50);
        assertEquals(3, forkJoinTree.size());

        forkJoinTree.delete(25);
        assertEquals(2, forkJoinTree.size());
        
        forkJoinTree.delete(100);
        assertEquals(2, forkJoinTree.size());
    }


    @Test
    void splitMaintainsInvariantsAndCorrectPartitioning() {
        ForkJoinTree<Integer> forkJoinTree = createRandomIntTree(250, 0, 250);

        List<Integer> splitPoints = List.of(7, 17, 25, 37, 50, 64, 75, 88, 90);
        for (Integer splitPoint : splitPoints) {
            Split<Integer> split = forkJoinTree.split(splitPoint);

            // Check invariants
            if (split.left().size() > 0) {
                assertTrue(areInvariantsSatisfied(split.left().root.get()));
            }

            if (split.right().size() > 0) {
                assertTrue(areInvariantsSatisfied(split.right().root.get()));
            }

            // Check correct partitioning
            for (Integer element : split.left()) {
                assertTrue(element < splitPoint, "Left split element " + element + " should be < " + splitPoint);
            }

            for (Integer element : split.right()) {
                assertTrue(element > splitPoint, "Right split element " + element + " should be > " + splitPoint);
            }
        }
    }


    @Test
    void unionOfDisjointSetsWorksCorrectly() {
        ForkJoinTree<Integer> forkJoinTree1 = new ForkJoinTree<>();
        IntStream.rangeClosed(1, 1000).forEach(forkJoinTree1::insert);

        ForkJoinTree<Integer> forkJoinTree2 = new ForkJoinTree<>();
        IntStream.rangeClosed(1001, 2000).forEach(forkJoinTree2::insert);

        ForkJoinTree<Integer> forkJoinTreeUnion = forkJoinTree1.union(forkJoinTree2);

        assertTrue(areInvariantsSatisfied(forkJoinTree1.root.get()));
        assertTrue(areInvariantsSatisfied(forkJoinTree2.root.get()));
        assertTrue(areInvariantsSatisfied(forkJoinTreeUnion.root.get()));

        assertEquals(2000, forkJoinTreeUnion.size());
        for (int i = 1; i <= 2000; i++) {
            assertTrue(forkJoinTreeUnion.contains(i), "Union should contain " + i);
        }

        assertEquals(1000, forkJoinTree1.size());
        assertEquals(1000, forkJoinTree2.size());
    }


    @Test
    void unionWithEmptyTreesWorksCorrectly() {
        ForkJoinTree<Integer> empty1 = new ForkJoinTree<>();
        ForkJoinTree<Integer> empty2 = new ForkJoinTree<>();
        ForkJoinTree<Integer> nonEmpty = createRandomIntTree(5, 0, 100);

        assertEquals(0, empty1.union(empty2).size());
        assertEquals(nonEmpty.size(), empty1.union(nonEmpty).size());
        assertEquals(nonEmpty.size(), nonEmpty.union(empty1).size());
    }


    @Test
    void unionWithOverlappingElementsWorksCorrectly() {
        ForkJoinTree<Integer> forkJoinTree1 = new ForkJoinTree<>();
        ForkJoinTree<Integer> forkJoinTree2 = new ForkJoinTree<>();

        List.of(10, 20, 30, 40).forEach(forkJoinTree1::insert);
        List.of(30, 40, 50, 60).forEach(forkJoinTree2::insert);

        ForkJoinTree<Integer> forkJoinTreeUnion = forkJoinTree1.union(forkJoinTree2);

        assertEquals(6, forkJoinTreeUnion.size());
        List.of(10, 20, 30, 40, 50, 60).forEach(element -> assertTrue(forkJoinTreeUnion.contains(element)));
        assertTrue(areInvariantsSatisfied(forkJoinTreeUnion.root.get()));
    }


    @Test
    void intersectionOfEmptyTreesReturnsEmpty() {
        ForkJoinTree<Integer> empty1 = new ForkJoinTree<>();
        ForkJoinTree<Integer> empty2 = new ForkJoinTree<>();

        assertEquals(0, empty1.intersection(empty2).size());
    }


    @Test
    void intersectionOfDisjointSetsReturnsEmpty() {
        ForkJoinTree<Integer> tree1 = new ForkJoinTree<>();
        ForkJoinTree<Integer> tree2 = new ForkJoinTree<>();

        List.of(1, 2, 3).forEach(tree1::insert);
        List.of(4, 5, 6).forEach(tree2::insert);

        assertEquals(0, tree1.intersection(tree2).size());
    }


    @Test
    void intersectionWithOverlappingElementsWorksCorrectly() {
        ForkJoinTree<Integer> forkJoinTree1 = new ForkJoinTree<>();
        IntStream.rangeClosed(100, 600).forEach(forkJoinTree1::insert);

        ForkJoinTree<Integer> forkJoinTree2 = new ForkJoinTree<>();
        IntStream.rangeClosed(300, 800).forEach(forkJoinTree2::insert);

        ForkJoinTree<Integer> intersection = forkJoinTree1.intersection(forkJoinTree2);

        assertEquals(301, intersection.size());
        IntStream.rangeClosed(300, 600).forEach(element -> assertTrue(intersection.contains(element)));
    }


    @Test
    void intersectionMatchesOracleImplementation() {
        TreeSet<Integer> oracle1 = new TreeSet<>();
        TreeSet<Integer> oracle2 = new TreeSet<>();
        ForkJoinTree<Integer> tree1 = new ForkJoinTree<>();
        ForkJoinTree<Integer> tree2 = new ForkJoinTree<>();

        // Create two overlapping random sets
        IntStream.range(0, 5000).forEach(i -> {
            int val = ThreadLocalRandom.current().nextInt(0, 3000);
            oracle1.add(val);
            tree1.insert(val);
        });

        IntStream.range(0, 5000).forEach(i -> {
            int val = ThreadLocalRandom.current().nextInt(150, 4500);
            oracle2.add(val);
            tree2.insert(val);
        });

        TreeSet<Integer> oracleResult = new TreeSet<>(oracle1);
        oracleResult.retainAll(oracle2);

        ForkJoinTree<Integer> treeResult = tree1.intersection(tree2);

        assertEquals(oracleResult.size(), treeResult.size());
        assertTreeEqualsSet(treeResult, oracleResult);
    }


    @Test
    void differenceOfEmptyTreesReturnsEmpty() {
        ForkJoinTree<Integer> empty1 = new ForkJoinTree<>();
        ForkJoinTree<Integer> empty2 = new ForkJoinTree<>();

        assertEquals(0, empty1.difference(empty2).size());
    }


    @Test
    void differenceOfDisjointSetsReturnsFirstSet() {
        ForkJoinTree<Integer> tree1 = new ForkJoinTree<>();
        ForkJoinTree<Integer> tree2 = new ForkJoinTree<>();

        List.of(1, 2, 3).forEach(tree1::insert);
        List.of(4, 5, 6).forEach(tree2::insert);

        ForkJoinTree<Integer> difference = tree1.difference(tree2);

        assertEquals(3, difference.size());
        List.of(1, 2, 3).forEach(element -> assertTrue(difference.contains(element)));
    }


    @Test
    void differenceOfIdenticalSetsReturnsEmpty() {
        ForkJoinTree<Integer> tree = createRandomIntTree(100, 0, 100);

        assertEquals(0, tree.difference(tree).size());
    }


    @Test
    void differenceWithOverlappingElementsWorksCorrectly() {
        ForkJoinTree<Integer> forkJoinTree1 = new ForkJoinTree<>();
        IntStream.rangeClosed(100, 600).forEach(forkJoinTree1::insert);

        ForkJoinTree<Integer> forkJoinTree2 = new ForkJoinTree<>();
        IntStream.rangeClosed(300, 800).forEach(forkJoinTree2::insert);

        ForkJoinTree<Integer> difference = forkJoinTree1.difference(forkJoinTree2);

        assertEquals(200, difference.size());
        IntStream.rangeClosed(100, 200).forEach(element -> assertTrue(difference.contains(element)));
        IntStream.rangeClosed(300, 800).forEach(element -> assertFalse(difference.contains(element)));
    }


    @Test
    void differenceMatchesOracleImplementation() {
        TreeSet<Integer> oracle1 = new TreeSet<>();
        TreeSet<Integer> oracle2 = new TreeSet<>();
        ForkJoinTree<Integer> tree1 = new ForkJoinTree<>();
        ForkJoinTree<Integer> tree2 = new ForkJoinTree<>();

        // Create two overlapping random sets
        IntStream.range(0, 500).forEach(i -> {
            int val = ThreadLocalRandom.current().nextInt(0, 300);
            oracle1.add(val);
            tree1.insert(val);
        });

        IntStream.range(0, 500).forEach(i -> {
            int val = ThreadLocalRandom.current().nextInt(150, 450);
            oracle2.add(val);
            tree2.insert(val);
        });

        TreeSet<Integer> oracleResult = new TreeSet<>(oracle1);
        oracleResult.removeAll(oracle2);

        ForkJoinTree<Integer> treeResult = tree1.difference(tree2);

        assertEquals(oracleResult.size(), treeResult.size());
        assertTreeEqualsSet(treeResult, oracleResult);
    }


    @Test
    void rangeQueryReturnsElementsWithinBounds() {
        ForkJoinTree<Integer> tree = createRandomIntTree(1000, 0, 1000);

        ForkJoinTree<Integer> range = tree.range(100, 200);

        assertTrue(areInvariantsSatisfied(range.root.get()));
        for (Integer element : range) {
            assertTrue(element >= 100 && element <= 200, "Range element " + element + " should be in [100, 200]");
        }
    }


    @Test
    void rangeQueryWithInvalidBoundsReturnsEmpty() {
        ForkJoinTree<Integer> tree = createRandomIntTree(100, 0, 100);

        ForkJoinTree<Integer> invalidRange = tree.range(200, 100); // high < low

        assertEquals(0, invalidRange.size());
    }


    @Test
    void rangeQueryOnEmptyTreeReturnsEmpty() {
        ForkJoinTree<Integer> tree = new ForkJoinTree<>();

        ForkJoinTree<Integer> range = tree.range(10, 20);

        assertEquals(0, range.size());
    }


    @Test
    void lowerThanFilterReturnsCorrectElements() {
        ForkJoinTree<Integer> tree = createRandomIntTree(1000, 0, 1000);
        ForkJoinTree<Integer> result = tree.lowerThan(250);

        for (Integer element : result) {
            assertTrue(element <= 250, "Element " + element + " should be <= 250");
        }
    }


    @Test
    void greaterThanFilterReturnsCorrectElements() {
        ForkJoinTree<Integer> tree = createRandomIntTree(1000, 0, 1000);
        ForkJoinTree<Integer> result = tree.greaterThan(250);

        for (Integer element : result) {
            assertTrue(element >= 250, "Element " + element + " should be >= 250");
        }
    }


    @Test
    void filterOperationsOnEmptyTreeReturnEmpty() {
        ForkJoinTree<Integer> tree = new ForkJoinTree<>();

        assertEquals(0, tree.lowerThan(50).size());
        assertEquals(0, tree.greaterThan(50).size());
    }


    @Test
    void complexMixedOperationsMaintainInvariants() {
        ForkJoinTree<Integer> tree = new ForkJoinTree<>();
        TreeSet<Integer> oracle = new TreeSet<>();

        for (int i = 0; i < 100; i++) {
            int value = ThreadLocalRandom.current().nextInt(0, 75);
            tree.insert(value);
            oracle.add(value);
        }

        assertTrue(areInvariantsSatisfied(tree.root.get()));
        assertEquals(oracle.size(), tree.size());

        for (int i = 0; i < 50; i += 2) {
            tree.delete(i);
            oracle.remove(i);
        }

        assertTrue(areInvariantsSatisfied(tree.root.get()));
        assertEquals(oracle.size(), tree.size());
        assertTreeEqualsSet(tree, oracle);
    }


    private List<String> generateRandomStrings(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
    }


    private ForkJoinTree<Integer> createRandomIntTree(int operationCount, int minValue, int maxValue) {
        ForkJoinTree<Integer> tree = new ForkJoinTree<>();
        IntStream.rangeClosed(1, operationCount).forEach(i -> {
            int value = ThreadLocalRandom.current().nextInt(minValue, maxValue);
            tree.insert(value);
        });

        return tree;
    }


    private ForkJoinTree<String> createRandomStringTree(int operationCount) {
        ForkJoinTree<String> forkJoinTree = new ForkJoinTree<>();
        for (String randomString : generateRandomStrings(operationCount)) {
            forkJoinTree.insert(randomString);
        }

        return forkJoinTree;
    }


    private void assertTreeEqualsSet(ForkJoinTree<Integer> tree, TreeSet<Integer> oracle) {
        Iterator<Integer> treeIterator = tree.iterator();
        for (Integer expected : oracle) {
            assertTrue(treeIterator.hasNext(), "Tree iterator should have more elements");
            assertEquals(expected, treeIterator.next());
        }

        assertFalse(treeIterator.hasNext(), "Tree iterator should be exhausted");
    }


    private static <T extends Comparable<T>> boolean areInvariantsSatisfied(TreeNode<T> root) {
        return checkInvariants(root).isValid;
    }


    private static <T extends Comparable<T>> InvariantCheckResult checkInvariants(TreeNode<T> root) {
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


    private static final class InvariantCheckResult {
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