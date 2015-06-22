package reqs;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by maksing on 14/6/15.
 * Wrap a Request instance. Every Session instance is unique in a Reqs object.
 */
public class RequestSession {
    private final Request request;
    private Reqs reqs;
    private Reqs subReqs;
    private final int id;

    private AtomicInteger currentRetryCount = new AtomicInteger(0);

    private volatile boolean isDone = false;

    /**
     * Constructor of the session
     * @param id a generated new id for the request session
     * @param reqs the current Reqs instance
     * @param request the request to be called in this session
     * @see Request
     */
    public RequestSession(int id, Reqs reqs, Request request) {
        this.reqs = reqs;
        this.request = request;
        this.id = id;
    }

    /**
     * return the request session id
     * @return the request session id
     */
    public int getId() {
        return id;
    }

    /**
     * Mark this request session as done
     * @param data the responded data
     */
    public synchronized void done(Object data) {
        if (!isDone) {
            isDone = true;
            reqs.requestDone(this, data);
        } else {
            throw new IllegalStateException("the session is done already!");
        }
    }

    /**
     * Mark this request session as failure, the flow will be interrupted and won't continue when
     * the max retry count is 0, but it will retry again if max retry count is > 0.
     * @param data the error response data
     */
    public synchronized void fail(Object data) {
        if (!isDone) {
            isDone = true;
            reqs.requestFailed(this, data);
        } else {
            throw new IllegalStateException("the session is done already!");
        }
    }

    /**
     * try to do the request again. be careful not to block the app by keep calling this function.
     */
    public void retry(Object errorData) {
        if (!isDone) {
            if (request.getMaxRetryCount() == 0) {
                call();
            } else if (currentRetryCount.get() < request.getMaxRetryCount()) {
                fail(errorData);
            }
        }
    }

    public synchronized void failNoRetry(Object data) {
        currentRetryCount.set(request.getMaxRetryCount());
        fail(data);
    }

    public boolean isLastRetry() {
        if (request.getMaxRetryCount() > 0) {
            return currentRetryCount.get() >= request.getMaxRetryCount() - 1;
        } else {
            return false;
        }
    }

    synchronized void resumeNext() {
        request.onNext(this, reqs.getResponseById(id));
    }

    void resumeSubReqs() {
        if (subReqs != null) {
            subReqs.resume();
        }
    }

    void pauseSubReqs() {
        if (subReqs != null && request.pausable) {
            subReqs.pause();
        }
    }

    void cancelSubReqs() {
        if (subReqs != null) {
            subReqs.cancel();
        }
    }

    public Request getRequest() {
        return request;
    }

    void next(final Response response) {
        request.onNext(this, response);
    }

    synchronized void call() {
        request.onCall(this);
    }

    public Reqs getReqs() {
        return reqs;
    }

    public int getRetryCount() {
        return currentRetryCount.get();
    }

    /**
     * set new retry count
     * @param currentRetryCount
     */
    public void setCurrentRetryCount(int currentRetryCount) {
        this.currentRetryCount.set(currentRetryCount);
    }

    void currentRetryCountAddOne() {
        this.currentRetryCount.getAndAdd(1);
    }

    public <T> T getLastStepData(Class<T> c) {
        return reqs.getLastStepData(c);
    }

    public <T> List<T> getLastStepDataList(Class<T> c) {
        return reqs.getLastStepDataList(c);
    }

    public Reqs getLastStepReqs() {
        return reqs.getLastStepData(Reqs.class);
    }

    void setSubReqs(Reqs subReqs) {
        this.subReqs = subReqs;
    }
}
