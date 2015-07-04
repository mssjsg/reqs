package com.maksing.reqsdemo;

import java.util.ArrayList;
import java.util.List;

import reqs.AsyncRequest;
import reqs.ForEachRequest;
import reqs.Reqs;
import reqs.Request;
import reqs.Response;

/**
 * Created by maksing on 4/7/15.
 */
public class AsyncRequestActivity extends BaseActivity {
    @Override
    protected Reqs getReqs() {
        Reqs reqs1 = Reqs.create(new ForEachRequest<Data>() {
            @Override
            public List getParamsList(Reqs reqs) {
                List<Data> datas = new ArrayList<Data>();
                for (int i = 0; i < 10; i++) {
                    datas.add(new Data("Async request " + i));
                }
                return datas;
            }

            @Override
            public Request getRequest(final Data param) {
                return new AsyncRequest<Data>() {
                    @Override
                    public Data doInBackground() {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

//                Integer.parseInt(null); //for testing exception

                        return param;
                    }

                    @Override
                    public void onDataResponded(Reqs reqs, Data result) {
                        log(result.str);
                    }
                };
            }
        }).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {


                log("total data responsed:" + reqs.getDataList(Data.class).size());
            }

            @Override
            public void onFailure(Response failedResponse) {
                log(failedResponse.getData().toString());
            }
        });

        return Reqs.create().thenReqs(reqs1, true);
    }
}
