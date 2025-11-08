package io.github.djordjijeK;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZI_Result;

import java.util.stream.IntStream;


@JCStressTest
@Outcome(id = "true, true, 499", expect = Expect.ACCEPTABLE)
@Outcome(expect = Expect.FORBIDDEN)
@State
public class ForkJoinTreeDifferenceTest extends BaseTest {
    private final ForkJoinTree<Integer> forkJoinTree1;
    private final ForkJoinTree<Integer> forkJoinTree2;


    public ForkJoinTreeDifferenceTest() {
        forkJoinTree1 = new ForkJoinTree<>();
        forkJoinTree2 = new ForkJoinTree<>();

        IntStream.rangeClosed(1, 1000).forEach(forkJoinTree1::insert);
        IntStream.rangeClosed(300, 800).forEach(forkJoinTree2::insert);
    }


    @Actor
    public void firstModifier() {
        IntStream.rangeClosed(10000, 20000).forEach(forkJoinTree2::insert);
    }


    @Actor
    public void secondModifier() {
        IntStream.rangeClosed(20000, 30000).forEach(forkJoinTree2::insert);
    }


    @Actor
    public void difference(ZZI_Result result) {
        ForkJoinTree<Integer> differenceForkJoinTree = forkJoinTree1.difference(forkJoinTree2);

        result.r1 = IntStream.rangeClosed(1, 299).allMatch(differenceForkJoinTree::contains) &&
                IntStream.rangeClosed(801, 1000).allMatch(differenceForkJoinTree::contains);
        result.r2 = checkInvariants(differenceForkJoinTree.root.get()).isValid;
        result.r3 = differenceForkJoinTree.size();
    }
}