package io.github.djordjijeK;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.openjdk.jcstress.infra.runners.ForkedTestConfig;
import org.openjdk.jcstress.infra.collectors.TestResult;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.infra.runners.WorkerSync;
import org.openjdk.jcstress.util.Counter;
import org.openjdk.jcstress.os.AffinitySupport;
import org.openjdk.jcstress.vm.AllocProfileSupport;
import org.openjdk.jcstress.infra.runners.FootprintEstimator;
import org.openjdk.jcstress.infra.runners.VoidThread;
import org.openjdk.jcstress.infra.runners.LongThread;
import org.openjdk.jcstress.infra.runners.CounterThread;
import io.github.djordjijeK.ForkJoinTreeInsertTest;
import org.openjdk.jcstress.infra.results.ZI_Result;

public final class ForkJoinTreeInsertTest_jcstress extends Runner<ZI_Result> {

    volatile WorkerSync workerSync;
    ForkJoinTreeInsertTest[] gs;
    ZI_Result[] gr;

    public ForkJoinTreeInsertTest_jcstress(ForkedTestConfig config) {
        super(config);
    }

    @Override
    public void sanityCheck(Counter<ZI_Result> counter) throws Throwable {
        sanityCheck_API(counter);
        sanityCheck_Footprints(counter);
    }

    private void sanityCheck_API(Counter<ZI_Result> counter) throws Throwable {
        final ForkJoinTreeInsertTest s = new ForkJoinTreeInsertTest();
        final ZI_Result r = new ZI_Result();
        VoidThread a0 = new VoidThread() { protected void internalRun() {
            s.writerOne();
        }};
        VoidThread a1 = new VoidThread() { protected void internalRun() {
            s.writerTwo();
        }};
        VoidThread a2 = new VoidThread() { protected void internalRun() {
            s.writerThree();
        }};
        a0.start();
        a1.start();
        a2.start();
        a0.join();
        if (a0.throwable() != null) {
            throw a0.throwable();
        }
        a1.join();
        if (a1.throwable() != null) {
            throw a1.throwable();
        }
        a2.join();
        if (a2.throwable() != null) {
            throw a2.throwable();
        }
            s.arbiter(r);
        counter.record(r);
    }

    private void sanityCheck_Footprints(Counter<ZI_Result> counter) throws Throwable {
        config.adjustStrideCount(new FootprintEstimator() {
          public void runWith(int size, long[] cnts) {
            long time1 = System.nanoTime();
            long alloc1 = AllocProfileSupport.getAllocatedBytes();
            ForkJoinTreeInsertTest[] ls = new ForkJoinTreeInsertTest[size];
            ZI_Result[] lr = new ZI_Result[size];
            for (int c = 0; c < size; c++) {
                ForkJoinTreeInsertTest s = new ForkJoinTreeInsertTest();
                ZI_Result r = new ZI_Result();
                lr[c] = r;
                ls[c] = s;
            }
            LongThread a0 = new LongThread() { public long internalRun() {
                long a1 = AllocProfileSupport.getAllocatedBytes();
                for (int c = 0; c < size; c++) {
                    ls[c].writerOne();
                }
                long a2 = AllocProfileSupport.getAllocatedBytes();
                return a2 - a1;
            }};
            LongThread a1 = new LongThread() { public long internalRun() {
                long a1 = AllocProfileSupport.getAllocatedBytes();
                for (int c = 0; c < size; c++) {
                    ls[c].writerTwo();
                }
                long a2 = AllocProfileSupport.getAllocatedBytes();
                return a2 - a1;
            }};
            LongThread a2 = new LongThread() { public long internalRun() {
                long a1 = AllocProfileSupport.getAllocatedBytes();
                for (int c = 0; c < size; c++) {
                    ls[c].writerThree();
                }
                long a2 = AllocProfileSupport.getAllocatedBytes();
                return a2 - a1;
            }};
            a0.start();
            a1.start();
            a2.start();
            try {
                a0.join();
                cnts[0] += a0.result();
            } catch (InterruptedException e) {
            }
            try {
                a1.join();
                cnts[0] += a1.result();
            } catch (InterruptedException e) {
            }
            try {
                a2.join();
                cnts[0] += a2.result();
            } catch (InterruptedException e) {
            }
            for (int c = 0; c < size; c++) {
                ls[c].arbiter(lr[c]);
            }
            for (int c = 0; c < size; c++) {
                counter.record(lr[c]);
            }
            long time2 = System.nanoTime();
            long alloc2 = AllocProfileSupport.getAllocatedBytes();
            cnts[0] += alloc2 - alloc1;
            cnts[1] += time2 - time1;
        }});
    }

    @Override
    public ArrayList<CounterThread<ZI_Result>> internalRun() {
        int len = config.strideSize * config.strideCount;
        gs = new ForkJoinTreeInsertTest[len];
        gr = new ZI_Result[len];
        for (int c = 0; c < len; c++) {
            gs[c] = new ForkJoinTreeInsertTest();
            gr[c] = new ZI_Result();
        }
        workerSync = new WorkerSync(false, 3, config.spinLoopStyle);

        control.isStopped = false;

        if (config.localAffinity) {
            try {
                AffinitySupport.tryBind();
            } catch (Exception e) {
                // Do not care
            }
        }

        ArrayList<CounterThread<ZI_Result>> threads = new ArrayList<>(3);
        threads.add(new CounterThread<ZI_Result>() { public Counter<ZI_Result> internalRun() {
            return task_writerOne();
        }});
        threads.add(new CounterThread<ZI_Result>() { public Counter<ZI_Result> internalRun() {
            return task_writerTwo();
        }});
        threads.add(new CounterThread<ZI_Result>() { public Counter<ZI_Result> internalRun() {
            return task_writerThree();
        }});

        for (CounterThread<ZI_Result> t : threads) {
            t.start();
        }

        if (config.time > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(config.time);
            } catch (InterruptedException e) {
            }
        }

        control.isStopped = true;

        return threads;
    }

    private void jcstress_consume(Counter<ZI_Result> cnt, int a) {
        ForkJoinTreeInsertTest[] ls = gs;
        ZI_Result[] lr = gr;
        int len = config.strideSize * config.strideCount;
        int left = a * len / 3;
        int right = (a + 1) * len / 3;
        for (int c = left; c < right; c++) {
            ZI_Result r = lr[c];
            ForkJoinTreeInsertTest s = ls[c];
            s.arbiter(r);
            ls[c] = new ForkJoinTreeInsertTest();
            cnt.record(r);
            r.r1 = false;
            r.r2 = 0;
        }
    }

    private void jcstress_sink(int v) {};
    private void jcstress_sink(short v) {};
    private void jcstress_sink(byte v) {};
    private void jcstress_sink(char v) {};
    private void jcstress_sink(long v) {};
    private void jcstress_sink(float v) {};
    private void jcstress_sink(double v) {};
    private void jcstress_sink(Object v) {};

    private Counter<ZI_Result> task_writerOne() {
        int len = config.strideSize * config.strideCount;
        int stride = config.strideSize;
        Counter<ZI_Result> counter = new Counter<>();
        if (config.localAffinity) AffinitySupport.bind(config.localAffinityMap[0]);
        while (true) {
            WorkerSync sync = workerSync;
            if (sync.stopped) {
                return counter;
            }
            int check = 0;
            for (int start = 0; start < len; start += stride) {
                run_writerOne(gs, gr, start, start + stride);
                check += 3;
                sync.awaitCheckpoint(check);
            }
            jcstress_consume(counter, 0);
            if (sync.tryStartUpdate()) {
                workerSync = new WorkerSync(control.isStopped, 3, config.spinLoopStyle);
            }
            sync.postUpdate();
        }
    }

    private void run_writerOne(ForkJoinTreeInsertTest[] gs, ZI_Result[] gr, int start, int end) {
        ForkJoinTreeInsertTest[] ls = gs;
        ZI_Result[] lr = gr;
        for (int c = start; c < end; c++) {
            ForkJoinTreeInsertTest s = ls[c];
            s.writerOne();
        }
    }

    private Counter<ZI_Result> task_writerTwo() {
        int len = config.strideSize * config.strideCount;
        int stride = config.strideSize;
        Counter<ZI_Result> counter = new Counter<>();
        if (config.localAffinity) AffinitySupport.bind(config.localAffinityMap[1]);
        while (true) {
            WorkerSync sync = workerSync;
            if (sync.stopped) {
                return counter;
            }
            int check = 0;
            for (int start = 0; start < len; start += stride) {
                run_writerTwo(gs, gr, start, start + stride);
                check += 3;
                sync.awaitCheckpoint(check);
            }
            jcstress_consume(counter, 1);
            if (sync.tryStartUpdate()) {
                workerSync = new WorkerSync(control.isStopped, 3, config.spinLoopStyle);
            }
            sync.postUpdate();
        }
    }

    private void run_writerTwo(ForkJoinTreeInsertTest[] gs, ZI_Result[] gr, int start, int end) {
        ForkJoinTreeInsertTest[] ls = gs;
        ZI_Result[] lr = gr;
        for (int c = start; c < end; c++) {
            ForkJoinTreeInsertTest s = ls[c];
            s.writerTwo();
        }
    }

    private Counter<ZI_Result> task_writerThree() {
        int len = config.strideSize * config.strideCount;
        int stride = config.strideSize;
        Counter<ZI_Result> counter = new Counter<>();
        if (config.localAffinity) AffinitySupport.bind(config.localAffinityMap[2]);
        while (true) {
            WorkerSync sync = workerSync;
            if (sync.stopped) {
                return counter;
            }
            int check = 0;
            for (int start = 0; start < len; start += stride) {
                run_writerThree(gs, gr, start, start + stride);
                check += 3;
                sync.awaitCheckpoint(check);
            }
            jcstress_consume(counter, 2);
            if (sync.tryStartUpdate()) {
                workerSync = new WorkerSync(control.isStopped, 3, config.spinLoopStyle);
            }
            sync.postUpdate();
        }
    }

    private void run_writerThree(ForkJoinTreeInsertTest[] gs, ZI_Result[] gr, int start, int end) {
        ForkJoinTreeInsertTest[] ls = gs;
        ZI_Result[] lr = gr;
        for (int c = start; c < end; c++) {
            ForkJoinTreeInsertTest s = ls[c];
            s.writerThree();
        }
    }

}
