package reqs;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by maksing on 18/6/15.
 */
public abstract class AsyncRequest extends AbstractAsyncRequest {
    public AsyncRequest() {
        Handler handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public Object doInBackground(RequestSession requestSession) {
        return null;
    }
}
