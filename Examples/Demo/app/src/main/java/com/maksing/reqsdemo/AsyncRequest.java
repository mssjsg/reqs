package com.maksing.reqsdemo;

import android.os.Handler;
import android.os.Looper;

import reqs.AbstractAsyncRequest;

/**
 * Created by maksing on 4/7/15.
 */
public abstract class AsyncRequest<T> extends AbstractAsyncRequest<T> {

    private Handler handler = new Handler(Looper.getMainLooper());

    public AsyncRequest() {
        super();
    }

    @Override
    protected void postOnMainThread(Runnable runnable) {
        handler.post(runnable);
    }
}
