package reqs;

import java.util.List;
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
public abstract class AbstractAsyncRequest<Data> extends Request {

    private final long timeout;

    public AbstractAsyncRequest() {
        timeout = -1;
    }

    protected AbstractAsyncRequest(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void onCall(final RequestSession requestSession) {

        final AsyncTaskRequest<Data> request = new AsyncTaskRequest<Data>(timeout) {

            @Override
            public Data doInBackground() throws Exception {
                return AbstractAsyncRequest.this.doInBackground();
            }
        };

        Reqs reqs = Reqs.create(request).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, final List<Response> responses) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        requestSession.done(responses.get(0).getData());
                    }
                };

                postOnMainThread(runnable);
            }

            @Override
            public void onFailure(final Response failedResponse) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        requestSession.fail(failedResponse.getData());
                    }
                };

                postOnMainThread(runnable);
            }
        }).setOnCancelListener(new Reqs.OnCancelListener() {
            @Override
            public void onCancel(Reqs reqs) {
                request.futureTask.cancel(true);
            }
        });
        reqs.start();
        requestSession.setSubReqs(reqs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void onNext(RequestSession requestSession, Response response) {
        super.onNext(requestSession, response);
        onDataResponded(requestSession.getReqs(), (Data) response.getData(Object.class));
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void onFailure(RequestSession requestSession, Response errorResponse) {
        super.onFailure(requestSession, errorResponse);
        onError(requestSession.getReqs(), (Throwable)errorResponse.getData());
    }

    public abstract Data doInBackground() throws Exception;

    public void onDataResponded(Reqs reqs, Data result) {

    }

    public void onError(Reqs reqs, Throwable error) {
        error.printStackTrace();
    }

    protected abstract void postOnMainThread(Runnable runnable);

    private static abstract class AsyncTaskRequest<Result> extends Request {

        private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
        private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
        private static final int KEEP_ALIVE = 1;

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

        private RequestSession requestSession;

        private AtomicBoolean taskInvoked = new AtomicBoolean();

        private final long timeout;

        private Callable worker = new Callable<Result>() {
            public Result call() throws Exception {
                return doInBackground();
            }
        };

        private FutureTask futureTask = new FutureTask<Result>(worker) {
            @Override
            protected void done() {
                Result result = null;
                try {
                    if (timeout != -1) {
                        result = get(timeout, TimeUnit.MILLISECONDS);
                    } else {
                        result = get();
                    }
                } catch (InterruptedException e) {
                } catch (ExecutionException e) {
                    error = e.getCause();
                } catch (TimeoutException e) {
                    error = e;
                } catch (CancellationException e) {;
                }

                postResultIfNotInvoked(requestSession, result);
            }
        };

        private Throwable error;

        protected AsyncTaskRequest(long timeout) {
            this.timeout = timeout;
        }

        private void postResultIfNotInvoked(RequestSession requestSession, Result result) {
            if (!taskInvoked.get()) {
                taskInvoked.set(true);
                postResult(requestSession, result);
            }
        }

        private Result postResult(final RequestSession requestSession, final Result result) {

            if (error != null) {
                requestSession.fail(error);
            } else {
                requestSession.done(result);
            }
            return result;
        }

        public abstract Result doInBackground() throws Exception;

        @Override
        public void onCall(final RequestSession requestSession) {
            this.requestSession = requestSession;
            sDefaultExecutor.execute(futureTask);
        }
    }
}
