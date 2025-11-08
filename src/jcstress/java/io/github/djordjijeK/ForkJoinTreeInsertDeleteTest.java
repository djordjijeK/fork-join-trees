package io.github.djordjijeK;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZI_Result;

import java.util.stream.IntStream;


@JCStressTest
@Outcome(id = "true, true, 10000", expect = Expect.ACCEPTABLE)
@Outcome(expect = Expect.FORBIDDEN)
@State
public class ForkJoinTreeInsertDeleteTest extends BaseTest {
    private final ForkJoinTree<Integer> forkJoinTree;


    public ForkJoinTreeInsertDeleteTest() {
        forkJoinTree = new ForkJoinTree<>();
        IntStream.rangeClosed(1, 10_000).forEach(forkJoinTree::insert);
    }


    @Actor
    public void insertOne() {
        IntStream.rangeClosed(10_001, 15_000).forEach(forkJoinTree::insert);
    }


    @Actor
    public void insertTwo() {
        IntStream.rangeClosed(15_000, 20_000).forEach(forkJoinTree::insert);
    }


    @Actor
    public void deleteOne() {
        IntStream.rangeClosed(1, 8_000).forEach(forkJoinTree::delete);
    }


    @Actor
    public void deleteTwo() {
        IntStream.rangeClosed(2_000, 10_000).forEach(forkJoinTree::delete);
    }


    @Arbiter
    public void arbiter(ZZI_Result result) {
        boolean deleted = IntStream.rangeClosed(1, 10_000).noneMatch(forkJoinTree::contains);
        boolean present = IntStream.rangeClosed(10_001, 20_000).allMatch(forkJoinTree::contains);

        result.r1 = deleted && present;
        result.r2 = checkInvariants(forkJoinTree.root.get()).isValid;
        result.r3 = forkJoinTree.size();
    }
}
