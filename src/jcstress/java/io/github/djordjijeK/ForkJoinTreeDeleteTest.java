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
@Outcome(id = "true, true, 1000", expect = Expect.ACCEPTABLE)
@Outcome(expect = Expect.FORBIDDEN)
@State
public class ForkJoinTreeDeleteTest extends BaseTest {
    private final ForkJoinTree<String> forkJoinTree;


    public ForkJoinTreeDeleteTest() {
        forkJoinTree = new ForkJoinTree<>();
        for (int i = 1; i <= 3000; i++) {
            forkJoinTree.insert(String.format("Test %d", i));
        }
    }


    @Actor
    public void writerOne() {
        IntStream.range(1, 1500)
                .mapToObj(integer -> String.format("Test %d", integer))
                .forEach(forkJoinTree::delete);
    }


    @Actor
    public void writerTwo() {
        IntStream.rangeClosed(500, 2000)
                .mapToObj(integer -> String.format("Test %d", integer))
                .forEach(forkJoinTree::delete);
    }


    @Arbiter
    public void arbiter(ZZI_Result result) {
        result.r1 = IntStream.rangeClosed(2001, 3000)
                .mapToObj(integer -> String.format("Test %d", integer))
                .allMatch(forkJoinTree::contains);
        result.r2 = checkInvariants(forkJoinTree.root.get()).isValid;
        result.r3 = forkJoinTree.size();
    }
}
