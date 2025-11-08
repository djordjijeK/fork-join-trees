# ForkJoinTree

A **lock-free, persistent, self-balancing binary search tree** implementation that leverages Java's ForkJoinPool for
highly efficient parallel set operations.

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Core Concepts](#core-concepts)
    - [Persistence and Immutability](#persistence-and-immutability)
    - [Concurrency via MVCC](#concurrency-via-mvcc-multi-version-concurrency-control)
    - [Parallelism via Fork/Join Framework](#parallelism-via-forkjoin-framework)
- [Building and Testing](#building-and-testing)

---

## Overview

`ForkJoinTree<T>` is a generic, immutable, and concurrency-optimized ordered set. It is designed to combine the
efficiency of balanced trees with the scalability of functional data structures.

It has the following defining properties:

- **Balanced Binary Search Tree:** The structure maintains balance through AVL-style rotations, guaranteeing logarithmic
  time complexity O(log n) for insertion, deletion, and lookup operations, regardless of update history.

- **Persistent with Structural Sharing:** Query operations - such as unions, intersections, range queries, and splits -
  return new `ForkJoinTree` instances that share unmodified structure with the original. Mutation operations (`insert`
  and `delete`) use atomic compare-and-swap to safely update the tree in-place. Both approaches leverage *structural
  sharing*, ensuring that only modified paths are recreated while unchanged subtrees are reused. This minimizes memory
  overhead and enables efficient versioning through query operations.

- **Concurrent and Parallel Execution:** The design is optimized for concurrent access. Reads are lock-free, implemented
  through a Multi-Version Concurrency Control (MVCC)-inspired mechanism that allows multiple threads to observe
  consistent snapshots without contention. The Java Fork/Join framework is used to parallelize bulk operations such as
  merges, traversals, and range queries, providing high throughput on multicore systems.

The result is a scalable, purely functional tree structure that combines immutability, efficient memory usage, and
fine-grained parallelism, making it suitable for both concurrent algorithms and functional-style programming.

---

## Features

`ForkJoinTree<T>` provides a comprehensive set of operations designed for efficiency, immutability, and parallel
scalability.

**Basic Operations**

- `insert(T element):` Adds the specified element to the tree using lock-free atomic operations. Multiple concurrent
  insertions are safely coordinated through compare-and-swap (CAS) loops.

- `delete(T element):` Removes the specified element from the tree using lock-free atomic operations. Concurrent
  deletions are handled safely without blocking.

- `contains(T element):` Determines whether an element exists within the tree in O(log n) time with no allocations.

- `size():` Returns the total number of elements currently present in O(1) time.

**Range Queries**

- `lowerThan(T element):` Produces a new tree containing all elements less than or equal to the specified element.
- `greaterThan(T element):` Produces a new tree containing all elements greater than or equal to the specified element.
- `range(T low, T high):` Returns a new tree containing all elements within the inclusive range `[low, high]`.

**Set Operations**

All set operations are internally parallelized through the Java Fork/Join framework, allowing efficient use of multicore
processors.

- `union(ForkJoinTree<T> other):` Returns a tree representing the union of both input trees.
- `intersection(ForkJoinTree<T> other):` Returns a tree containing only elements present in both trees.
- `difference(ForkJoinTree<T> other):` Returns a tree containing elements from the first tree that are not in the
  second.

**Splitting**

- `split(T element):` Divides the tree into two disjoint trees - one containing all elements smaller than the given key,
  and another containing all elements greater. Both resulting trees share unmodified substructure with the original.

**Iteration**

- `iterator():` Provides an ascending-order iterator over the elements.
- `iteratorDescending():` Provides a descending-order iterator.

All operations preserve immutability through structural sharing, ensuring logarithmic-time updates, efficient memory
use, and full thread safety without synchronization.

---

## Core Concepts

Understanding the following concepts is key to leveraging the full potential of `ForkJoinTree`.

### Persistence and Immutability

The `ForkJoinTree` achieves persistence through *structural sharing*, a technique that enables efficient versioning and
snapshot isolation.

When query operations like `union()`, `range()`, or `greaterThan()` are called, they return new `ForkJoinTree`
instances. These new trees share unchanged subtrees with the original, and only the nodes that differ are newly
allocated. This is known as *path copying* - only the nodes along the path to modified elements are recreated.

The primary benefit is the ability to create immutable snapshots. Any reference you hold to a `ForkJoinTree` represents
a consistent, immutable view of the data at that point in time. You can pass these snapshots to other threads or use
them for long-running computations, confident they will never change - even if the original tree is later modified
through `insert()` or `delete()` operations.

```java
ForkJoinTree<Integer> v1 = new ForkJoinTree<>();
v1.

insert(10);
v1.

insert(20);
v1.

insert(30);
// v1 now contains: {10, 20, 30}

// Create a new tree containing only elements greater than 15
ForkJoinTree<Integer> v2 = v1.greaterThan(15);
// v2 contains: {20, 30}
// v1 is unchanged: {10, 20, 30}

// Create another new tree from a range query on v1
ForkJoinTree<Integer> v3 = v1.range(10, 20);
// v3 contains: {10, 20}
// v1 and v2 remain unaffected

// At this point, you have three distinct, immutable snapshots:
// v1: {10, 20, 30}
// v2: {20, 30}
// v3: {10, 20}
```

### Concurrency via MVCC (Multi-Version Concurrency Control)

The snapshot-based design of `ForkJoinTree` enables a concurrency model similar to MVCC, a technique widely used in
high-performance databases like PostgreSQL and Oracle.

The core principle is simple: **readers don't block writers, and writers don't block readers**.

When you capture a reference to a tree (or derive a new tree via a query operation), you hold an immutable snapshot.
Other threads can continue to modify the original tree through `insert()` and `delete()` without affecting your
snapshot. Similarly, reads never require locks - threads simply access the current atomic root reference.

This MVCC-inspired approach eliminates traditional locking for read operations, dramatically reducing contention and
improving throughput in applications with many concurrent readers.

### Parallelism via Fork/Join Framework

While concurrency is about managing access from multiple threads, parallelism is about actively distributing work across
multiple CPU cores to accelerate computation.

`ForkJoinTree` leverages Java's Fork/Join framework to parallelize its most computationally intensive operations -
specifically `union()`, `intersection()`, and `difference()`. These set operations recursively subdivide work into
smaller tasks that execute concurrently on available processor cores, achieving near-linear speedup on multi-core
systems for large datasets.

This parallelism is transparent to the caller and automatically scales with the number of available cores, making bulk
operations significantly faster without requiring any special handling in application code.

## Building and Testing

### Prerequisites

- Java 17 or higher
- Gradle 7.0+

### Build Commands

```bash
# Compile the project
./gradlew build

# Run unit tests
./gradlew test

# Run JCStress concurrency tests
./gradlew jcstress
```
