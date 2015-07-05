package reqs;

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

    @Override
    protected final void postOnMainThread(Runnable runnable) {
        handler.post(runnable);
    }
}
