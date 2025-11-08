package io.github.djordjijeK;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZI_Result;

import java.util.stream.IntStream;


@JCStressTest
@Outcome(id = "true, true, 3001", expect = Expect.ACCEPTABLE)
@Outcome(expect = Expect.FORBIDDEN)
@State
public class ForkJoinTreeIntersectionTest extends BaseTest {
    private final ForkJoinTree<Integer> forkJoinTree1;
    private final ForkJoinTree<Integer> forkJoinTree2;


    public ForkJoinTreeIntersectionTest() {
        forkJoinTree1 = new ForkJoinTree<>();
        forkJoinTree2 = new ForkJoinTree<>();

        IntStream.rangeClosed(1000, 5000).forEach(forkJoinTree1::insert);
        IntStream.rangeClosed(2000, 7000).forEach(forkJoinTree2::insert);
    }


    @Actor
    public void firstModifier() {
        IntStream.rangeClosed(1, 999).forEach(forkJoinTree1::insert);
    }


    @Actor
    public void secondModifier() {
        IntStream.rangeClosed(7_001, 10_000).forEach(forkJoinTree2::insert);
    }


    @Actor
    public void difference(ZZI_Result result) {
        ForkJoinTree<Integer> intersectionForkJoinTree = forkJoinTree1.intersection(forkJoinTree2);

        result.r1 = IntStream.rangeClosed(2000, 5000).allMatch(intersectionForkJoinTree::contains);
        result.r2 = checkInvariants(intersectionForkJoinTree.root.get()).isValid;
        result.r3 = intersectionForkJoinTree.size();
    }
}