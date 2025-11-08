package io.github.djordjijeK;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZI_Result;

import java.util.stream.IntStream;


@JCStressTest
@Outcome(id = "true, true, 6001", expect = Expect.ACCEPTABLE)
@Outcome(expect = Expect.FORBIDDEN)
@State
public class ForkJoinTreeUnionTest extends BaseTest {
    private final ForkJoinTree<Integer> forkJoinTree1;
    private final ForkJoinTree<Integer> forkJoinTree2;


    public ForkJoinTreeUnionTest() {
        forkJoinTree1 = new ForkJoinTree<>();
        forkJoinTree2 = new ForkJoinTree<>();

        IntStream.rangeClosed(1000, 5000).forEach(forkJoinTree1::insert);
        IntStream.rangeClosed(2000, 7000).forEach(forkJoinTree2::insert);
    }


    @Actor
    public void firstModifier() {
        IntStream.rangeClosed(2000, 7000).forEach(forkJoinTree1::insert);
    }


    @Actor
    public void secondModifier() {
        IntStream.rangeClosed(1000, 5000).forEach(forkJoinTree2::insert);
    }


    @Actor
    public void difference(ZZI_Result result) {
        ForkJoinTree<Integer> unionForkJoinTree = forkJoinTree1.union(forkJoinTree2);

        result.r1 = IntStream.rangeClosed(1000, 7000).allMatch(unionForkJoinTree::contains);
        result.r2 = checkInvariants(unionForkJoinTree.root.get()).isValid;
        result.r3 = unionForkJoinTree.size();
    }
}