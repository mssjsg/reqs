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
    public void Reqs_CreateOneRequestAndStart_CorrectResponse() {

        int result[] = {123, 321, 456};

        final ReqsTester tester = getTester(result);
        final Reqs reqs = Reqs.create(getAsyncRequest(result[0]), getAsyncRequest(result[1])).then(getSyncRequest(result[2])).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {
                for (int i = 0; i < responses.size(); i++) {
                    tester.setActualValue(i, reqs.getResponseById(i).getData(Integer.class));
                }
            }

            @Override
            public void onFailure(Response failedResponse) {

            }
        });
        reqs.start();
        tester.check();
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
