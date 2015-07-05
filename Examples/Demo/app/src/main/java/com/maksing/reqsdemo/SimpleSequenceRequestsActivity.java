package com.maksing.reqsdemo;

import android.os.Handler;
import android.os.Looper;

import java.util.List;

import reqs.AsyncRequest;
import reqs.Reqs;
import reqs.Request;
import reqs.RequestSession;
import reqs.Response;

/**
 * Created by maksing on 5/7/15.
 */
public class SimpleSequenceRequestsActivity extends BaseActivity {
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected Reqs getReqs() {
        return Reqs.create().then(new Request() {

            @Override
            public void onCall(RequestSession requestSession) {
                delayedForgroundWork("Step 1", 1000, requestSession);
            }

            @Override
            public void onNext(RequestSession requestSession, Response response) {
                super.onNext(requestSession, response);
                log(response.getData(Data.class).str);
            }
        }).then(new Request() {

            @Override
            public void onCall(RequestSession requestSession) {
                delayedForgroundWork("Step 2", 500, requestSession);
            }

            @Override
            public void onNext(RequestSession requestSession, Response response) {
                super.onNext(requestSession, response);
                log(response.getData(Data.class).str);
            }
        }).then(new Request() {

            @Override
            public void onCall(RequestSession requestSession) {
                delayedForgroundWork("Step 3", 1000, requestSession);
            }

            @Override
            public void onNext(RequestSession requestSession, Response response) {
                super.onNext(requestSession, response);
                log(response.getData(Data.class).str);
            }
        }).then(new AsyncRequest<Data>() {
            @Override
            public Data doInBackground() throws InterruptedException {
                return backgroundWork("Step 4 async", 1000);
            }

            @Override
            public void onDataResponded(Reqs reqs, Data result) {
                super.onDataResponded(reqs, result);
                log(result.str);
            }
        }).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {
                log("done!");
            }

            @Override
            public void onFailure(Response failedResponse) {

            }
        });
    }

    private void delayedForgroundWork(final String str, long time, final RequestSession requestSession) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                requestSession.done(new Data(str));
            }
        }, time);
    }

    private Data backgroundWork(String str, long time) throws InterruptedException {
        Thread.sleep(time);
        return new Data(str);
    }
}
