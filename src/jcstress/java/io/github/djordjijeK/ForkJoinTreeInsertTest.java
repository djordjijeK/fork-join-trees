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
@Outcome(id = "true, true, 3000", expect = Expect.ACCEPTABLE)
@Outcome(expect = Expect.FORBIDDEN)
@State
public class ForkJoinTreeInsertTest extends BaseTest {
    private final ForkJoinTree<Integer> forkJoinTree = new ForkJoinTree<>();


    @Actor
    public void writerOne() {
        IntStream.range(1, 1000).forEach(forkJoinTree::insert);
    }


    @Actor
    public void writerTwo() {
        IntStream.range(1000, 2000).forEach(forkJoinTree::insert);
    }


    @Actor
    public void writerThree() {
        IntStream.rangeClosed(2000, 3000).forEach(forkJoinTree::insert);
    }


    @Arbiter
    public void arbiter(ZZI_Result result) {
        result.r1 = IntStream.rangeClosed(1, 3000).allMatch(forkJoinTree::contains);
        result.r2 = checkInvariants(forkJoinTree.root.get()).isValid;
        result.r3 = forkJoinTree.size();
    }
}