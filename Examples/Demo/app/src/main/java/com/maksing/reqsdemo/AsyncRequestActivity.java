package com.maksing.reqsdemo;

import java.util.List;

import reqs.Reqs;
import reqs.Response;

/**
 * Created by maksing on 4/7/15.
 */
public class AsyncRequestActivity extends BaseActivity {
    @Override
    protected Reqs getReqs() {
        Reqs reqs1 = Reqs.create(new AsyncRequest<Data>() {
            @Override
            public Data doInBackground() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

//                Integer.parseInt(null); //for testing exception

                return new Data("ha");
            }

            @Override
            public void onDataResponded(Reqs reqs, Data result) {
                log(result.str);
            }
        }).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {
                log(reqs.getData(Data.class).str);
            }

            @Override
            public void onFailure(Response failedResponse) {
                log(failedResponse.getData().toString());
            }
        });

        return Reqs.create().thenReqs(reqs1, true);
    }
}
