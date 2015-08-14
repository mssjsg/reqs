package reqs;

import android.os.Process;
import android.os.Handler;
import android.os.Looper;

/**
 * Created by maksing on 4/7/15.
 * handle the async operation and post result on Android main thread
 */
public abstract class AsyncRequest<T> extends AbstractAsyncRequest<T> {

    private Handler handler = new Handler(Looper.getMainLooper());

    public AsyncRequest() {
        super();
    }

    /**
     * Do the background task and returned the data.
     * @return data in background thread
     * @throws Exception
     */
    public final T getDataInBackground(Reqs reqs) throws Exception {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        return doInBackground(reqs);
    }

    /**
     * Do the background task and returned the data.
     * @return data in background thread
     * @throws Exception
     */
    public abstract T doInBackground(Reqs reqs) throws Exception;

    @Override
    protected final void postOnMainThread(Runnable runnable) {
        handler.post(runnable);
    }
}
