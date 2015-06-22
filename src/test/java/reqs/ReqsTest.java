package reqs;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by maksing on 20/6/15.
 */
public class ReqsTest {
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Test
    public void basicTest1() {

        int result[] = {123, 321, 456};

        final ReqsTester tester = getTester(result);

        Reqs.create(getAsyncRequest(result[0]), getAsyncRequest(result[1]))
                .then(getSyncRequest(result[2]))
                .done(new BasicOnDoneListner(tester))
                .start();

        tester.check();
    }

    @Test
    public void getLastStepOneRequest() {

        final int result[] = {67};

        final ReqsTester tester = getTester(result);

        Reqs.create(getAsyncRequest(result[0]))
                .then(new Request() {
                    @Override
                    public void onCall(RequestSession requestSession) {
                        tester.setActualValue(0, requestSession.getLastStepData(Integer.class));
                    }
                })
                .start();

        tester.check();
    }

    @Test
    public void getLastStep6Requests_responsesOrderCorrect() {

        final int result[] = {67, 78, 54, 34, 54, 43};

        final ReqsTester tester = getTester(result);

        Reqs.create(getAsyncRequest(result[0]), getAsyncRequest(result[1]), getAsyncRequest(result[2]), getAsyncRequest(result[3]), getAsyncRequest(result[4]), getAsyncRequest(result[5]))
                .then(new Request() {
                    @Override
                    public void onCall(RequestSession requestSession) {
                        List<Integer> list = requestSession.getLastStepDataList(Integer.class);
                        for (int i = 0; i < list.size(); i++) {
                            tester.setActualValue(i, list.get(i));
                        }
                    }
                }).start();

        tester.check();
    }

    @Test
    public void getLastStepDeepLevelsReqsRequest() {

        final int result[] = {67};

        final ReqsTester tester = getTester(result);

        Reqs.create().thenReqs(Reqs.create().thenReqs(Reqs.create().thenReqs(Reqs.create().thenReqs(Reqs.create().then(getAsyncRequest(result[0]))))))
                .then(new Request() {
                    @Override
                    public void onCall(RequestSession requestSession) {
                        tester.setActualValue(0, requestSession.getLastStepData(Integer.class));
                    }
                })
                .start();

        tester.check();
    }

    @Test
    public void getMultiStepsLastStepOneReqsRequest() {

        final int result[] = {67, 90};

        final ReqsTester tester = getTester(result);

        Reqs.create().thenReqs(Reqs.create().then(getAsyncRequest(result[0]))).then(new Request() {
            @Override
            public void onCall(RequestSession requestSession) {
                tester.setActualValue(0, requestSession.getLastStepData(Integer.class));
                requestSession.done(null);
            }
        }).thenReqs(Reqs.create().then(getAsyncRequest(result[1])))
                .then(new Request() {
                    @Override
                    public void onCall(RequestSession requestSession) {
                        tester.setActualValue(1, requestSession.getLastStepData(Integer.class));
                    }
                }).start();

        tester.check();
    }

    class BasicOnDoneListner implements Reqs.OnDoneListener {

        private ReqsTester tester;

        public BasicOnDoneListner(ReqsTester tester) {
            this.tester = tester;
        }

        @Override
        public void onSuccess(Reqs reqs, List<Response> responses) {
            for (Response response : responses) {
                tester.setActualValue(response.getRequestSessionId(), response.getData(Integer.class));
            }
        }

        @Override
        public void onFailure(Response failedResponse) {

        }
    }

    private AsyncRequest getAsyncRequest(int value) {
        return new AsyncRequest(value);
    }

    private SyncRequest getSyncRequest(int value) {
        return new SyncRequest(value);
    }

    class AsyncRequest extends Request {

        private final int value;

        AsyncRequest(int value) {
            this.value = value;
        }

        @Override
        public void onCall(RequestSession requestSession) {
            doAsyncRequest(requestSession, value);
        }
    }

    class SyncRequest extends Request {

        private final int value;

        SyncRequest(int value) {
            this.value = value;
        }

        @Override
        public void onCall(RequestSession requestSession) {
            doSyncRequest(requestSession, value);
        }
    }

    private ReqsTester getTester(int[] expectedResult) {
        return new ReqsTester(expectedResult);
    }

    private ReqsTester getTester(int[] expectedResult, int sleepInterval, int timeout) {
        return new ReqsTester(expectedResult, sleepInterval, timeout);
    }

    private void doSyncRequest(final RequestSession requestSession, final int result) {
        requestSession.done(result);
    }

    private void doAsyncRequest(final RequestSession requestSession, final int result) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                requestSession.done(result);
            }
        };

        executorService.execute(task);
    }
}
