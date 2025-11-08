package io.github.djordjijeK;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZZ_Result;

import java.util.stream.IntStream;


@JCStressTest
@Outcome(id = "true, true", expect = Expect.ACCEPTABLE)
@Outcome(expect = Expect.FORBIDDEN)
@State
public class ForkJoinTreeRangeTest extends BaseTest {
    private final ForkJoinTree<Integer> forkJoinTree;


    public ForkJoinTreeRangeTest() {
        forkJoinTree = new ForkJoinTree<>();
    }


    @Actor
    public void firstModifier() {
        IntStream.rangeClosed(1, 1000).forEach(forkJoinTree::insert);
    }


    @Actor
    public void secondModifier() {
        IntStream.rangeClosed(1000, 2000).forEach(forkJoinTree::insert);
    }


    @Actor
    public void thirdModifier() {
        IntStream.rangeClosed(3000, 5000).forEach(forkJoinTree::insert);
    }


    @Actor
    public void difference(ZZ_Result result) {
        ForkJoinTree<Integer> ltForkJoinTree = forkJoinTree.lowerThan(2500);
        ForkJoinTree<Integer> gtForkJoinTree = forkJoinTree.greaterThan(2500);
        ForkJoinTree<Integer> rangeForkJoinTree = forkJoinTree.range(1000, 3000);

        boolean ltResult = true;
        boolean gtResult = true;
        boolean rangeResult = true;

        for (int value : ltForkJoinTree) {
            if (value > 2500) {
                ltResult = false;
                break;
            }
        }

        for (int value : gtForkJoinTree) {
            if (value < 2500) {
                gtResult = false;
                break;
            }
        }

        for (int value : rangeForkJoinTree) {
            if (value < 1000 || value > 3000) {
                rangeResult = false;
                break;
            }
        }

        result.r1 = ltResult && gtResult && rangeResult;
        result.r2 = checkInvariants(ltForkJoinTree.root.get()).isValid && checkInvariants(ltForkJoinTree.root.get()).isValid && checkInvariants(rangeForkJoinTree.root.get()).isValid;
    }
}
