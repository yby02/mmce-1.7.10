package github.kasuminova.mmce.common.util.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicLong;

public class CustomForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    private static final AtomicLong THREAD_ID = new AtomicLong(0);

    private final String threadName;

    public CustomForkJoinWorkerThreadFactory(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public ForkJoinWorkerThread newThread(final ForkJoinPool pool) {
        CustomForkJoinWorkerThread thread = new CustomForkJoinWorkerThread(pool);
        thread.setName(String.format(threadName, THREAD_ID.getAndIncrement()));
        return thread;
    }

    public static class CustomForkJoinWorkerThread extends ForkJoinWorkerThread {
        public CustomForkJoinWorkerThread(final ForkJoinPool pool) {
            super(pool);
        }
    }
}
