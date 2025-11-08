# ForkJoinTree

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A **lock-free, persistent, self-balancing binary search tree** implementation that leverages Java's ForkJoinPool for
highly efficient parallel set operations.

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

- 🔒 **Lock-Free Mutations** - Non-blocking insertions and deletions
- 📸 **Immutable Snapshots** - Create consistent point-in-time views without copying the entire tree
- ⚡ **Parallel Operations** - Automatic multicore utilization for set operations
- ⚖️ **Self-Balancing Tree** - Maintains optimal structure for guaranteed performance
- 🔍 **Efficient Range Queries** - Fast lookups and filtering operations
- 🔄 **Bidirectional Iteration** - Traverse elements in ascending or descending order
- 🧵 **Thread-Safe** - Safe concurrent access without explicit synchronization
- 💾 **Memory Efficient** - Structural sharing minimizes memory overhead

---

## Core Concepts

Understanding these three pillars will help you leverage `ForkJoinTree` effectively.

### Persistence and Immutability

`ForkJoinTree` achieves **persistence through structural sharing** - a technique that enables efficient versioning and
snapshot isolation.

When you perform operations like `union()`, `range()`, or `greaterThan()`, a new tree is returned that shares unchanged
subtrees with the original. Only nodes along modified paths are recreated (**path copying**).

**Key Benefits:**

- Create immutable snapshots that never change
- Share snapshots safely across threads
- No defensive copying required
- Minimal memory overhead through structural reuse

```java
ForkJoinTree<Integer> v1 = new ForkJoinTree<>();
v1.insert(10);
v1.insert(20);
v1.insert(30);
// v1: {10, 20, 30}

ForkJoinTree<Integer> v2 = v1.greaterThan(15);
// v2: {20, 30}
// v1 unchanged: {10, 20, 30}

ForkJoinTree<Integer> v3 = v1.range(10, 20);
// v3: {10, 20}
// v1 and v2 remain unaffected
```

### Concurrency via MVCC

The snapshot-based design enables **Multi-Version Concurrency Control** - the same technique used by high-performance
databases like PostgreSQL and Oracle.

**Core Principle:** Readers don't block writers, and writers don't block readers.

- Readers access immutable snapshots without any locks
- Writers use atomic compare-and-swap to update the root reference
- Failed CAS operations retry with the new snapshot
- No traditional locking or synchronization required

This MVCC-inspired approach eliminates read contention and dramatically improves throughput in read-heavy workloads.

### Parallelism via Fork/Join Framework

`ForkJoinTree` automatically parallelizes computationally intensive operations across available CPU cores.

**Parallelized Operations:**

- `union()` - Merge two trees using divide-and-conquer
- `intersection()` - Find common elements in parallel
- `difference()` - Compute set difference across cores
- `range()` - Process left and right subtrees concurrently

The Fork/Join framework provides:

- Transparent work distribution
- Work-stealing for load balancing
- Near-linear speedup on multicore systems
- No manual thread management required

Performance scales automatically with available processor cores without any changes to application code.

---

## API Overview

| Operation                             | Description                                                                      |
|---------------------------------------|----------------------------------------------------------------------------------|
| `insert(T element)`                   | Add an element to the tree                                                       |
| `delete(T element)`                   | Remove an element from the tree                                                  |
| `contains(T element)`                 | Check if an element exists in the tree                                           |
| `size()`                              | Get the total number of elements                                                 |
| `union(ForkJoinTree<T> other)`        | Merge two trees into one containing all elements                                 |
| `intersection(ForkJoinTree<T> other)` | Create a tree containing only common elements                                    |
| `difference(ForkJoinTree<T> other)`   | Create a tree with elements in first tree but not in second                      |
| `range(T low, T high)`                | Create a tree containing elements within the specified range                     |
| `lowerThan(T element)`                | Create a tree containing elements less than or equal to the specified element    |
| `greaterThan(T element)`              | Create a tree containing elements greater than or equal to the specified element |
| `split(T element)`                    | Partition the tree into two disjoint trees around the specified element          |
| `iterator()`                          | Get an iterator for ascending-order traversal                                    |
| `iteratorDescending()`                | Get an iterator for descending-order traversal                                   |

---

## Building and Testing

### Prerequisites

- Java 17 or higher
- Gradle 7.0+

### Build Commands

```bash
# Clone the repository
git clone https://github.com/djordjijeK/fork-join-trees.git
cd fork-join-trees

# Compile the project
./gradlew build

# Run unit tests (functional correctness)
./gradlew test

# Run JCStress concurrency stress tests
./gradlew jcstress
```