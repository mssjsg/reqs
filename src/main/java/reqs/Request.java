package reqs;

import reqs.util.Debugger;

import java.util.List;

/**
 * Created by maksing on 14/6/15.
 * Implement this abstract class to define what to do in the request.
 */
public abstract class Request {
    protected final Class<?> expectResponseType;
    protected int maxRetryCount;
    protected boolean pausable = false;

    /**
     * Default constructor, no validation of the response type
     */
    public Request() {
        this(null);
    }

    /**
     * Constructor, the response data type will need to match expectResponseType or the flow will be interrupted.
     * @param expectResponseType the class type to match the response data.
     */
    public Request(Class<?> expectResponseType) {
        this.expectResponseType = expectResponseType;
    }

    /**
     * Define what to do in the request. session object is provided so that session.done(data) or session.fail(errorData) can be called to finish the request session.
     * @param requestSession the session instance that wrap this request.
     */
    public abstract void onCall(RequestSession requestSession);

    /**
     * Can override to provide handle when the request is done successfully before proceed to next request in the flow.
     * @param requestSession
     * @param response
     */
    public void onNext(RequestSession requestSession, Response response) {
        Debugger.log("Request " + requestSession.getId() + " responsed!! class: " + (response.getData() == null ? "null" : response.getData().getClass() + " value: " + response.getData()));
    }

    /**
     * Can override to provide handling of failure of this request
     * @param errorResponse
     */
    public void onFailure(RequestSession requestSession, Response errorResponse) {
        Debugger.log("Failed error response:" + errorResponse.getData());
    }

    /**
     * Retry the request after Session.fail. please note that retries cannot be paused so it will continue to retry even Reqs.pause is called
     * as the retires together are treated as one request in the current logic.
     * @param retryCount number of retries to do after this request failed before proceed to next step.
     * @return a new Request object that can do retry.
     */
    public Request retry(int retryCount) {
        return new RetryRequest(this, retryCount);
    }

    public Class<?> getExpectResponseType() {
        return expectResponseType;
    }

    /**
     * return the max retry count
     * @return the max retry count
     */
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    static class RetryRequest extends Request {
        private RequestSession requestSession;
        private Request request;

        public RetryRequest(final Request request, final int maxRetryCount) {
            this.maxRetryCount = maxRetryCount;
            this.pausable = true;
            this.request = request;
        }

        private void doRequest(final Request request) {

            Reqs reqs = Reqs.create(new Request() {
                @Override
                public void onCall(RequestSession requestSession) {
                    RetrySession retrySession = new RetrySession(requestSession, RetryRequest.this.requestSession.getReqs());
                    retrySession.setCurrentRetryCount(RetryRequest.this.requestSession.getRetryCount());
                    request.onCall(retrySession);
                }
            }).done(new Reqs.OnDoneListener() {
                @Override
                public void onSuccess(Reqs reqs, List<Response> responses) {
                    requestSession.done(reqs.getAllResponses().size() == 0 ? null : reqs.getAllResponses().get(0).getData());
                }

                @Override
                public void onFailure(Response failedResponse) {
                    if (requestSession.getRetryCount() < maxRetryCount) {
                        requestSession.currentRetryCountAddOne();
                        request.onFailure(requestSession, failedResponse);
                        doRequest(request);
                    } else {
                        requestSession.fail(failedResponse);
                    }
                }
            });

            requestSession.setSubReqs(reqs);
            reqs.start();
        }

        @Override
        public void onCall(RequestSession requestSession) {
            this.requestSession = requestSession;
            doRequest(request);
        }

        @Override
        public void onNext(RequestSession requestSession, Response data) {
            request.onNext(requestSession, data);
        }

        static class RetrySession extends RequestSession {

            Reqs mainReqs;
            RequestSession requestSession;

            public RetrySession(RequestSession requestSession, Reqs mainReqs) {
                super(requestSession.getId(), requestSession.getReqs(), requestSession.getRequest());
                this.requestSession = requestSession;
                this.mainReqs = mainReqs;
            }

            @Override
            public void done(Object data) {
                super.done(data);
            }

            @Override
            public void fail(Object data) {
                requestSession.fail(data);
            }

            /**
             * return the real Reqs object, don't call this inside this class!!!!
             * @return
             */
            @Override
            public Reqs getReqs() {
                return mainReqs;
            }
        }
    }
}