package io.github.djordjijeK;

public record Split<T extends Comparable<T>>(
        T element,
        ForkJoinTree<T> left,
        ForkJoinTree<T> right
) {
}