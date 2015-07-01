package reqs;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by maksing on 14/6/15.
 * this is still in progress, please dont use this
 */
public abstract class AbstractAsyncRequest<Result> extends Request {
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;

    private FutureTask<Result> futureTask;

    private long timeout = 10000;

    private Callable mWorker;

    private AtomicBoolean mTaskInvoked = new AtomicBoolean();

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncRequest #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);

    /**
     * An {@link Executor} that can be used to execute tasks in parallel.
     */
    public static final ExecutorService THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);

    private static volatile ExecutorService sDefaultExecutor = THREAD_POOL_EXECUTOR;

    public AbstractAsyncRequest() {
        super();
        mWorker = new Callable<Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);
                //noinspection unchecked
                return postResult(doInBackground());
            }
        };

        futureTask = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get(timeout, TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occured while executing doInBackground()", e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                } catch (TimeoutException e) {
                }
            }
        };
    }

    private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }

    private Result postResult(Result result) {
        postResultOnUiThread(result);
        return result;
    }

    @Override
    public void onCall(final RequestSession requestSession) {
        sDefaultExecutor.execute(futureTask);
    }

    public abstract Result doInBackground();

    protected abstract void postResultOnUiThread(RequestSession requestSession, Result result);

    private class ReqsFutureTask extends FutureTask<Result> {

        RequestSession requestSession;

        public ReqsFutureTask(RequestSession requestSession, Callable<Result> callable) {
            super(callable);
            this.requestSession = requestSession;
        }

        @Override
        protected void done() {
            try {
                postResultIfNotInvoked(get(timeout, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
                throw new RuntimeException("An error occured while executing doInBackground()", e.getCause());
            } catch (CancellationException e) {
                postResultIfNotInvoked(null);
            } catch (TimeoutException e) {
            }
        }
    }
}
