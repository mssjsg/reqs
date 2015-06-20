package reqs;

import reqs.Request;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import reqs.Reqs;
import reqs.RequestSession;
import reqs.Response;

/**
 * Created by maksing on 7/6/15.
 */
public class GeneralReqsTest {

    private static final String ERROR_SUFFIX = "Error";
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Test
    public void Reqs_CreateRequests_RequestsAdded() {
        final Reqs reqs = Reqs.create(requestA1, requestA2, requestA3, requestA4);
        Assert.assertEquals(reqs.requestLists.size(), 1);
        Assert.assertEquals(reqs.requestLists.get(0).list.size(), 4);
    }

    @Test
    public void Reqs_CreateReqsAndThenAddRequests_RequestsAdded() {
        final Reqs reqs = Reqs.create().then(requestA1, requestA2, requestA3, requestA4);
        Assert.assertEquals(reqs.requestLists.size(), 1);
        Assert.assertEquals(reqs.requestLists.get(0).list.size(), 4);
    }

    @Test
    public void Reqs_CreateOneRequestAndStart_CorrectResponse() {
        final Reqs reqs = Reqs.create(requestA1).done(getOnDoneListener("A1"));
        reqs.start();
    }

    @Test
    public void Reqs_CreateTwoRequestAndStart_CorrectResponses() {
        final Reqs reqs = Reqs.create(requestA1, requestA1).done(getOnDoneListener("A1", "A1"));
        reqs.start();
    }

    @Test
    public void Reqs_CreateMultiSameRequests_ResponseMultiSameRespones() {
        final Reqs reqs = Reqs.create(requestB1, requestB1, requestB1).done(getOnDoneListener("B1", "B1", "B1"));
        reqs.start();
    }

    @Test
    public void Reqs_CreateB1ThenB1ThenB1_ResponseB1B1B1() {
        final Reqs reqs = Reqs.create(requestB1).then(requestB1).then(requestB1).done(getOnDoneListener("B1", "B1", "B1"));
        reqs.start();
    }

    @Test
    public void Reqs_CreateA1A2A3_ResponseA1A2A3() {
        final Reqs reqs = Reqs.create(requestA1, requestA2, requestA3).done(getOnDoneListener("A1", "A2", "A3"));
        reqs.start();
    }

    @Test
    public void Reqs_CreateThenA1A2ThenA1A2A3ThenB1_ResponseA1A2A1A2A3B1() {
        final Reqs reqs = Reqs.create().then(requestA1, requestA2).then(requestA1, requestA2, requestA3).then(requestB1).done(getOnDoneListener("A1", "A2", "A1", "A2", "A3", "B1"));
        reqs.start();
    }

    @Test
    public void Reqs_CreateThenA1A2PauseResumeThenA1A2A3ThenB1_ResponseA1A2A1A2A3B1() {
        final Reqs reqs = Reqs.create().then(requestA1, requestA2).then(requestA1, requestA2, requestA3).then(requestB1).done(getOnDoneListener("A1", "A2", "A1", "A2", "A3", "B1"));
        reqs.start();
        reqs.pause();
        reqs.resume();
        Assert.assertFalse(reqs.pendingPerformCurrent);
    }

    @Test
    public void Reqs_CreateThenA1PauseResumeThenC1PauseResumeBeforeC1Respond_ResponseA1C1() {
        final Reqs reqs = Reqs.create().then(requestA1).then(requestC1).done(getOnDoneListener("A1", "C1"));
        reqs.start();
        reqs.pause();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                Assert.assertTrue(reqs.pendingPerformCurrent);
                reqs.resume();
                Assert.assertFalse(reqs.pendingPerformCurrent);
                reqs.pause();
                reqs.resume();
            }
        }, 30, TimeUnit.MILLISECONDS);
    }

    @Test
    public void Reqs_CreateThenA1A2Pause1secResumeThenA1A2A3_ResponseA1A2A1A2A3() {
        final Reqs reqs = Reqs.create().then(requestA1, requestA2).then(requestA1, requestA2, requestA3).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {
                Assert.assertEquals(responses.size(), 5);
                Assert.assertEquals(responses.get(0), "A1");
                Assert.assertEquals(responses.get(1), "A2");
                Assert.assertEquals(responses.get(2), "A1");
                Assert.assertEquals(responses.get(3), "A2");
                Assert.assertEquals(responses.get(4), "A3");
            }

            @Override
            public void onFailure(Response failedResponse) {

            }
        });
        reqs.start();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                reqs.pause();
            }
        }, 10, TimeUnit.MILLISECONDS);

        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                Assert.assertEquals(reqs.getAllResponses().size(), 2);
                Assert.assertTrue(reqs.pendingPerformCurrent);
                reqs.resume();
                Assert.assertFalse(reqs.pendingPerformCurrent);
            }
        }, 30, TimeUnit.MILLISECONDS);

        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                reqs.pause();
            }

        }, 35, TimeUnit.MILLISECONDS);

        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                Assert.assertEquals(reqs.getAllResponses().size(), 5);
                Assert.assertTrue(reqs.pendingCompletion);
                reqs.resume();
            }

        }, 100, TimeUnit.MILLISECONDS);

//        assertResponseData(reqs, "A1", "A2", "A1", "A2", "A3");
    }

    @Test
    public void Reqs_CreateA1ThenF1ThenA2_FailRequestId1() {
        final Reqs reqs = Reqs.create(requestA1).then(requestF1).then(requestA2).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {

            }

            @Override
            public void onFailure(Response failedResponse) {
                Assert.assertEquals(failedResponse.getData(), "F1" + ERROR_SUFFIX);
                assertResponseData(failedResponse.getReqs(), "A1", "F1" + ERROR_SUFFIX);
            }
        });
        reqs.start();

    }

    @Test
    public void Reqs_CreateA1ThenF1F11ThenA2_FailRequestId1() {
        final Reqs reqs = Reqs.create(requestA1).then(requestF1, requestF11).then(requestA2).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {
                Assert.fail();
            }

            @Override
            public void onFailure(Response failedResponse) {
                Assert.assertEquals(failedResponse.getData(), "F1" + ERROR_SUFFIX);
                assertResponseData(failedResponse.getReqs(), "A1", "F1" + ERROR_SUFFIX);
            }
        });
        reqs.start();
    }

    @Test
    public void Reqs_CreateA1ThenF2_FailRequestId1() {
        final Reqs reqs = Reqs.create(requestA1).then(requestF2).then(requestA2).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {
                Assert.fail();
            }

            @Override
            public void onFailure(Response failedResponse) {
                Assert.assertEquals(failedResponse.getData(), "F2" + ERROR_SUFFIX);
                assertResponseData(failedResponse.getReqs(), "A1", "F2" + ERROR_SUFFIX);
            }
        });
        reqs.start();

    }

    /**
     * Tests Thread safety
     */

    @Test
    public void Reqs_Create2RequestsOn2Threads_ResponseCorrectly() {
        final Reqs reqs = Reqs.create(requestT1, requestT2, requestT1, requestT1, requestT1).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {
                for (Response response : responses) {
                    System.out.printf("response:" + response.getData());
                }
                Assert.assertEquals(5, responses.size());
            }

            @Override
            public void onFailure(Response failedResponse) {
                Assert.fail();
            }
        });
        reqs.start();
        long startTime = System.currentTimeMillis();
        long timeout = 1000;
        while (true) {
            if (reqs.getAllResponses().size() == 5
                    && reqs.getAllResponses().get(0).getData().equals("T1")
                    && reqs.getAllResponses().get(1).getData().equals("T2")) {
                System.out.printf("response:" + reqs.getAllResponses().get(0).getData());
                System.out.printf("response:" + reqs.getAllResponses().get(1).getData());

                break;
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                Assert.fail("Timeout!!!");
            }
        }
    }

    /**
     * Some requests for testings
     */

    private Request requestA1 = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            doRequest(requestSession, "A1");
        }
    };

    private Request requestA2 = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            doRequest(requestSession, "A2");
        }
    };

    private Request requestA3 = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            doRequest(requestSession, "A3");
        }
    };

    private Request requestA4 = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            doRequest(requestSession, "A4");
        }
    };

    private Request requestB1 = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            requestSession.done("B1");
        }
    };

    private Request requestT1 = new Request() {
        @Override
        public void onCall(final RequestSession requestSession) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    requestSession.done("T1");
                }
            }).start();
        }
    };

    private Request requestT2 = new Request() {
        @Override
        public void onCall(final RequestSession requestSession) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    requestSession.done("T2");
                }
            }).start();
        }
    };

    private Request requestF1 = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            requestSession.fail("F1" + ERROR_SUFFIX);
        }
    };

    private Request requestF11 = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            requestSession.fail("F11" + ERROR_SUFFIX);
        }
    };

    private Request requestF2 = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            doFailRequest(requestSession, "F2");
        }
    };

    private Request requestC1 = new Request() {
        @Override
        public void onCall(RequestSession requestSession) {
            doLongRequest(requestSession, "C1");
        }
    };

    private void doRequest(final RequestSession requestSession, final String data) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                sleep(20);
                requestSession.done(data);
            }
        };

        executorService.execute(task);
    }

    private void doFailRequest(final RequestSession requestSession, final String data) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sleep(20);
                requestSession.fail(data + ERROR_SUFFIX);
            }
        };
        executorService.execute(runnable);
    }

    private void doLongRequest(final RequestSession requestSession, final String data) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sleep(100);
                requestSession.done(data);
            }
        };
        executorService.execute(runnable);
    }

    private void assertResponseData(final Reqs reqs, final String... responses) {
        for (int i = 0; i < responses.length; i++) {
            Assert.assertEquals(reqs.getAllResponses().get(i).getData(), responses[i]);
        }
        Assert.assertEquals(reqs.getAllResponses().size(), responses.length);
    }

    private ResponseCallback getResponseData(Reqs reqs, final int responsePos) {

        return new ResponseCallback(reqs) {
            @Override
            String onResponse(Reqs reqs) {
                try {
                    return reqs.getAllResponses().get(responsePos).getData(String.class);
                } catch (NullPointerException e) {
                    return null;
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
            }
        };
    }

    private Callable<Integer> getErrorResponseData(final Reqs reqs) {

        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                try {
                    return reqs.failedRequestId;
                } catch (NullPointerException e) {
                    return null;
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
            }
        };
    }

    private Reqs.OnDoneListener getOnDoneListener(final String... responses) {
        return new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses1) {
                assertResponseData(reqs, responses);
            }

            @Override
            public void onFailure(Response failedResponse) {

            }
        };
    }

    private Callable<Integer> getResponsesSize(final Reqs reqs) {

        return new Callable<Integer> () {

            @Override
            public Integer call() throws Exception {
                return reqs.getAllResponses().size();
            }
        };
    }

    private Reqs.OnDoneListener emptyDoneListener = new Reqs.OnDoneListener() {
        @Override
        public void onSuccess(Reqs reqs, List<Response> responses) {

        }

        @Override
        public void onFailure(Response failedResponse) {

        }
    };

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {

        }
    }

    private abstract class ResponseCallback implements Callable<String> {

        private final Reqs reqs;

        private ResponseCallback(Reqs reqs) {
            this.reqs = reqs;
        }

        @Override
        public String call() throws Exception {
            return onResponse(reqs);
        }

        abstract String onResponse(Reqs reqs);
    }

}
