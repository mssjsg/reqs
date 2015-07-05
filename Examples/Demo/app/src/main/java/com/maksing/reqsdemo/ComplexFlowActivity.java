package com.maksing.reqsdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import reqs.Reqs;
import reqs.ReqsRequest;
import reqs.Request;
import reqs.RequestSession;
import reqs.Response;


public class ComplexFlowActivity extends BaseActivity {

    private TextView textLog;

    private Handler handler = new Handler(Looper.getMainLooper());

    private Request requestX = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            doRequest("X", requestSession);
        }
    };

    private Request requestY = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            doRequest("Y", requestSession);
        }
    };

    private List<Request> requestList = new ArrayList<>();
    {
        requestList.add(requestX);
        requestList.add(requestY);
    }

    private Reqs reqs1 = Reqs.create(new Request() {
        @Override
        public void onCall(RequestSession session) {
            doRequest("initializing", session);
        }
    }, new Request() {
        @Override
        public void onCall(RequestSession session) {

            doRequest("signin...", session);
        }
    }).then(new Request() {

        @Override
        public void onCall(RequestSession session) {
            Data data = session.getLastStepData(Data.class);
            List<Data> dataList = session.getLastStepDataList(Data.class);

            if (session.getRetryCount() == 3) {
                doRequest("retry success!!", session);
            } else {
                doRequest("", session);
            }
        }

        @Override
        public void onFailure(RequestSession session, Response errorResponse) {
            super.onFailure(session, errorResponse);
            log("Failure!!! retry:" + session.getRetryCount());
        }
    }.retry(3)).next(new Reqs.OnNextListener() {
        @Override
        public void onNext(Reqs reqs, List<Response> responses) {
            log("I am still here.");
        }
    }).then(new ReqsRequest(Reqs.create(new Request() {
        @Override
        public void onCall(RequestSession session) {
            doRequest("sub flow 1", session);
        }
    })), new ReqsRequest(Reqs.create(new Request() {
        @Override
        public void onCall(RequestSession session) {
            doRequest("sub flow 2", session);
        }
    }))).thenReqs(Reqs.create(new Request() {
        @Override
        public void onCall(RequestSession session) {
            doRequest("sub flow 3", session);
        }
    })).switchRequests(new Reqs.OnSwitchListener() {
        @Override
        public Request onSwitch(Reqs reqs) {

            boolean happy = true;

            if (happy) {
                return new Request() {
                    @Override
                    public void onCall(RequestSession session) {
                        session.done(new Data("happy"));
                    }
                };
            } else {
                return new Request() {
                    @Override
                    public void onCall(RequestSession session) {
                        session.done(new Data("sad"));
                    }
                };
            }

        }
    }, new Reqs.OnSwitchListener() {
        @Override
        public Request onSwitch(Reqs reqs) {
            return null;
        }
    }).done(new Reqs.OnDoneListener() {
        @Override
        public void onSuccess(Reqs reqs, List<Response> responses) {
            String resp = "";

            List<Data> dataList = reqs.getDataList(Data.class);
            for (Data data : dataList) {
                resp = resp + data.str + "\n";
            }

            log("all requests done. responses:\n" + resp);

            log("flow 1 done. do flow 2 now");
        }

        @Override
        public void onFailure(Response failedResponse) {
            log("flow 1 failed");
        }
    }).setOnCancelListener(new Reqs.OnCancelListener() {
        @Override
        public void onCancel(Reqs reqs) {
            log("cancelled");
        }
    }).setOnPauseListener(new Reqs.OnPauseListener() {
        @Override
        public void onPause(Reqs reqs) {
            log("paused");
        }
    }).setOnResumeListener(new Reqs.OnResumeListener() {
        @Override
        public void onResume(Reqs reqs) {
            log("resume");
        }
    });

    private Reqs reqs2 = Reqs.create().then(new Request() {
        @Override
        public void onCall(RequestSession session) {
            doRequest("1", session);
        }

        @Override
        public void onNext(RequestSession session, Response response) {
            super.onNext(session, response);

            log(response.getData(Data.class).toString());
        }
    }, new Request() {
        @Override
        public void onCall(RequestSession session) {
            doRequest("2", session);
        }
    }, new Request() {
        @Override
        public void onCall(RequestSession session) {
            doRequest("3", session);
        }
    }).next(new Reqs.OnNextListener() {
        @Override
        public void onNext(Reqs reqs, List<Response> responses) {
            log("Request 1, 2, 3 done! response size:" + responses.size());
        }
    }).then(new Request() {

        @Override
        public void onCall(RequestSession session) {
            doRequest("4", session);
        }
    }).then(new Request() {

        @Override
        public void onCall(RequestSession session) {
            doRequest("5", session);
        }
    }).then(requestList).next(new Reqs.OnNextListener() {
        @Override
        public void onNext(Reqs reqs, List<Response> responses) {
            log("Request X, Y done! response size:" + responses.size());
        }
    }).done(new Reqs.OnDoneListener() {
        @Override
        public void onSuccess(Reqs reqs, List<Response> responses) {
            String resp = "";

            List<Data> dataList = reqs.getDataList(Data.class);

            for (Data data : dataList) {
                resp = resp + data.str + "\n";
            }

            log("all requests done. responses:\n" + resp);
        }

        @Override
        public void onFailure(Response failedResponse) {
            log("Requests Failed, cannot continue.");
        }
    }).setOnCancelListener(new Reqs.OnCancelListener() {
        @Override
        public void onCancel(Reqs reqs) {
            log("cancelled");
        }
    }).setOnPauseListener(new Reqs.OnPauseListener() {
        @Override
        public void onPause(Reqs reqs) {
            log("paused");
        }
    }).setOnResumeListener(new Reqs.OnResumeListener() {
        @Override
        public void onResume(Reqs reqs) {
            log("resume");
        }
    });

    private void doRequest(final String data, final RequestSession requestSession) {

        log("Start request " + data);

        final Callback<Data, String> callback = new Callback<Data, String>() {
            @Override
            public void onSuccess(Data response) {
//                log("responsed: " + response.str);
                requestSession.done(response);
            }

            @Override
            public void onFailure(String response) {
//                log("Request " + data + " failed" + response);
                requestSession.fail(response);
            }
        };

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(data)) {
                    callback.onFailure("Empty String!!");
                } else {
                    callback.onSuccess(new Data(data));
                }
            }
        }, 2000);
    }

    @Override
    protected Reqs getReqs() {
        return Reqs.create().thenReqs(reqs1, true).thenReqs(reqs2, true);
    }
}
