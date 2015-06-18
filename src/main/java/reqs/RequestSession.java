package reqs;

import java.util.Collections;
import java.util.List;

/**
 * Created by maksing on 14/6/15.
 * Wrap a Request instance. Every Session instance is unique in a Reqs object.
 * @param <E> The expected data type of the response.
 */
public class RequestSession<E> {
    private final Request<E> request;
    private Reqs reqs;
    private final int id;

    private int currentRetryCount = 0;

    private boolean isDone = false;

    /**
     * Constructor of the session
     * @param id a generated new id for the request session
     * @param reqs the current Reqs instance
     * @param request the request to be called in this session
     * @see Request
     */
    public RequestSession(int id, Reqs reqs, Request<E> request) {
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
    public void done(E data) {
        if (!isDone) {
            isDone = true;
            reqs.requestDone(this, data);
        } else {
            throw new IllegalStateException("the session is done already!");
        }
    }

    /**
     * Mark this request session as failure, the flow will be interrupted and won't continue.
     * @param data the error response data
     */
    public void fail(Object data) {
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
        if (request.getMaxRetryCount() == 0) {
            call();
        } else if (currentRetryCount < request.getMaxRetryCount()) {
            fail(errorData);
        }
    }

    public void failNoRetry(Object data) {
        currentRetryCount = request.getMaxRetryCount();
        fail(data);
    }

    void resume() {
        request.onNext(this, (E) reqs.getResponseById(id).getData());
    }

    public Request<E> getRequest() {
        return request;
    }

    <T> void next(final T data) {
        try {
            request.onNext(this, (E) data);
        } catch (ClassCastException e) {

        }
    }

    void call() {
        request.onCall(this);
    }

    public Reqs getReqs() {
        return reqs;
    }

    public int getRetryCount() {
        return currentRetryCount;
    }

    /**
     * set new retry count
     * @param currentRetryCount
     */
    public void setCurrentRetryCount(int currentRetryCount) {
        this.currentRetryCount = currentRetryCount;
    }

    public Reqs getLastStepReqs() {
        return reqs.getLastStepData(Reqs.class);
    }

    public List<Reqs> getLastStepReqsList() {
        return reqs.getLastStepReqsList();
    }

}
