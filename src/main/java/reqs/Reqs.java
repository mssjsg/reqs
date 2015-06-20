package reqs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import reqs.util.Debugger;
import reqs.util.SparseArray;

/**
 * Created by Sing Mak on 31/5/15.
 * A simple utility class to handle the life cycle, flow and responses of multiple requests.
 * @author Sing Mak (mssjsg@gmail.com)
 */
public final class Reqs {

    /**
     * the current state of this Reqs
     */
    enum State {
        /**
         * initial state before calling start()
         */
        IDLE,
        /**
         * after calling start() or when this Reqs is resumed
         */
        REQUESTING,
        /**
         * after calling cancel()
         */
        CANCELLED,
        /**
         * after calling pause()
         */
        PAUSED,
        /**
         * when all requests are done
         */
        COMPLETED
    }

    private List<RequestsMap> initialRequests = new ArrayList<>();
    List<RequestsMap> requestLists = new ArrayList<>();
    private SparseArray<Response> responses = new SparseArray<>();
    SparseArray<Response> currentStepResponses = new SparseArray<>();
    SparseArray<Response> lastStepResponses = new SparseArray<>();

    private List<RequestSession> pendingRequestLists = new ArrayList<>();

    private volatile State state = State.IDLE;

    private volatile boolean failed = false;

    volatile boolean pendingCompletion = false;
    volatile boolean pendingPerformCurrent = false;
    volatile private boolean calling = false;
    volatile int failedRequestId;

    private OnNextListener pendingNext;
    private List<Response> pendingRequestDone = new ArrayList<>();
    private Response pendingRequestFail;

    private OnDoneListener onDoneListener;
    private OnCancelListener onCancelListener;
    private OnPauseListener onPauseListener;
    private OnResumeListener onResumeListener;

    /**
     * Place holder request object that do nothing
     */
    public static Request EMPTY_REQUEST = new Request() {

        @Override
        public void onCall(RequestSession requestSession) {
            requestSession.done(null);
        }
    };

    /**
     * Returns a new instance of Reqs, add Requests to be called in parallel in the first step and create new Session objects for each Request. For example, the following idiom
     * create a new Reqs object and added 2 requests to be executed in parallel:
     * <pre>
     *      Reqs reqs = Reqs.create(request1, request2);
     * </pre>
     * @param requests Requests to be called in parallel in the first step.
     * @return A new instance of Reqs
     */
    public static Reqs create(Request... requests) {
        if (requests.length > 0) {
            return new Reqs(requests);
        } else {
            return new Reqs();
        }
    }

    /**
     * Return a new Reqs with the provided Reqs by copying the requests in it.
     * @return
     */
    public static Reqs createWithReqs(Reqs reqs) {
        Reqs reqs1 = Reqs.create();
        for (RequestsMap map : reqs.initialRequests) {
            List<RequestSession> requestSessions = map.list.getObjects();

            Request[] requestsArr = new Request[requestSessions.size()];
            for (int i = 0; i < requestSessions.size(); i++) {
                RequestSession requestSession = requestSessions.get(i);
                requestsArr[i] = requestSession.getRequest();
            }
            reqs1.then(requestsArr).next(reqs.initialRequests
                    .get(reqs1.requestLists.size() - 1).onNextListener);
        }

        reqs1.onCancelListener = reqs.onCancelListener;
        reqs1.onDoneListener = reqs.onDoneListener;
        reqs1.onPauseListener = reqs.onPauseListener;
        reqs1.onResumeListener = reqs.onResumeListener;

        return reqs1;
    }

    public static <Data> Data getLastStepData(RequestSession session, Class<Data> c) {
        return session.getReqs().getLastStepData(c);
    }

    public static <Data> List<Data> getLastStepDataList(RequestSession session, Class<Data> c) {
        return session.getReqs().getLastStepDataList(c);
    }

    public static <E> E getData(RequestSession session, Class<E> c) {
        return session.getReqs().getData(c);
    }

    public static <E> List<E> getDataList(RequestSession session, Class<E> c) {
        return session.getReqs().getDataList(c);
    }

    public static Reqs getLastStepReqs(RequestSession session) {
        return session.getLastStepReqs();
    }

    /**
     * short hand for cancel the reqs and do check null
     * @param reqs
     */
    public static void cancel(Reqs reqs) {
        if (reqs != null) {
            reqs.cancel();
        }
    }

    /**
     * short hand for resuming the reqs and do check null
     * @param reqs
     */
    public static void resume(Reqs reqs) {
        if (reqs != null) {
            reqs.resume();
        }
    }

    /**
     * short hand for pausing the reqs and do check null
     * @param reqs
     */
    public static void pause(Reqs reqs) {
        if (reqs != null) {
            reqs.pause();
        }
    }

    /**
     * Returns a new instance of Reqs, add Requests to be called in parallel in the first stepand create new Session objects for each Request.
     * @param requests List of Requests to be called in parallel in the first step.
     * @return A new instance of Reqs
     */
    public static Reqs create(List<Request> requests) {
        return create().then(requests);
    }

    private Reqs(List<Request> requests) {
        if (requests.size() > 0) {
            addRequests(requests);
        }
    }

    private Reqs(Request... requests) {
        if (requests.length > 0) {
            addRequests(requests);
        }
    }

    /**
     * to treat a requests flow a sub-flow of this Reqs object so that pausing the main flow won't interrupt this sub flow
     * @param reqs
     * @return the current Reqs object
     */
    public Reqs thenReqs(final Reqs reqs) {
        return then(new ReqsRequest(reqs));
    }

    /**
     * Add new Requests to the current Reqs instance and create new Session objects for each Request
     * @param requests List of Requests to be added
     * @return the current Reqs instance
     */
    public Reqs then(List<Request> requests) {
        return then(convertToRequestsArray(requests));
    }

    /**
     * Add new Requests to the current Reqs instance and create new Session objects for each Request
     * @param requests Requests to be added
     * @return the current Reqs instance
     */
    public Reqs then(Request... requests) {
        if (state == State.IDLE && requests.length > 0) {
            addRequests(requests);
        }
        return this;
    }

    /**
     * Define the Request to do based on the current data.
     * @param onSwitchListeners Define the Request to do based on the current data.
     * @return the Request to do then
     */
    public Reqs switchRequests(OnSwitchListener... onSwitchListeners) {
        List<Request> list = new ArrayList<>();
        for (OnSwitchListener onSwitchListener : onSwitchListeners) {
            list.add(new SwitchRequest(onSwitchListener));
        }

        return then(list);
    }

    Request[] convertToRequestsArray(List<Request> requests) {
        Request[] requests1 = new Request[requests.size()];
        for (int i = 0; i < requests.size(); i++) {
            requests1[i] = requests.get(i);
        }
        return requests1;
    }

    private void addRequests(List<Request> requests) {
        addRequests(convertToRequestsArray(requests));
    }

    private void addRequests(Request[] requests) {
        int id = getNextId();
        requestLists.add(new RequestsMap(id, this, requests));
        initialRequests.add(new RequestsMap(id, this, requests));
    }

    /**
     * Set the listener to be called when the all the Request Sessions are marked done or when one of the Request Sessions failed.
     * @param onDoneListener
     * @return current Reqs instance
     */
    public Reqs done(OnDoneListener onDoneListener) {
        this.onDoneListener = onDoneListener;
        return this;
    }

    /**
     * Start the flow after the Reqs instance has been setup.
     */
    public void start() {
        if (state == State.IDLE) {
            if (requestLists.size() == 0) {
                complete();
            } else {
                state = State.REQUESTING;
                performCurrent(false);
            }
        }
    }

    /**
     * Declare what to do next after all the requests in a step have responded.
     * @param onNextListener
     * @return the current instance of Reqs
     */
    public Reqs next(OnNextListener onNextListener) {
        if (requestLists.size() > 0) {
            requestLists.get(requestLists.size() - 1).onNextListener = onNextListener;
            initialRequests.get(requestLists.size() - 1).onNextListener = onNextListener;
        }
        return this;
    }

    private int getNextId() {
        int base = 0;
        for (RequestsMap map : requestLists) {
            base += map.list.size();
        }
        return base;
    }

    private void performCurrent(boolean noCheckForPending) {
        if (state == State.REQUESTING) {
            RequestsMap requestsMap = requestLists.get(0);
            calling = true;
            for (int i = 0; i < requestsMap.list.size(); i++) {
                SparseArray<RequestSession> map = requestsMap.list;
                RequestSession requestSession = map.get(map.keyAt(i));
                requestSession.call();
                log("session.request.onCall: requestsMap.list:" + requestsMap.list.size());
            }

            calling = false;

            if (!noCheckForPending) {
                if (pendingRequestDone.size() > 0) {
                    while (pendingRequestDone.size() > 0) {
                        log("pendingRequestDone: " + pendingRequestDone.size());
                        Response response = pendingRequestDone.get(0);
                        pendingRequestDone.remove(0);
                        requestDone(response.getRequestSession(), response.getData(), true);
                    }
                }
            }

            if (pendingRequestFail != null) {
                Response response = pendingRequestFail;
                pendingRequestFail = null;
                requestFailed(response.getRequestSession(), response.getData());
            }

        }
    }

    public synchronized <E> void requestDone(RequestSession requestSession, E data) {
        requestDone(requestSession, data, false);
    }

    /**
     * Return <tt>true</tt> if all the requests were responded.
     * @return <tt>true</tt> if all the requests were responded.
     */
    public boolean isCompleted() {
//        return requestLists.size() == 0 && hasStarted && !pendingCompletion;
        return state == State.COMPLETED;
    }

    /**
     * Return <tt>true</tt> if the requests flow is cancelled.
     * @return <tt>true</tt> if the requests flow is cancelled.
     */
    public boolean isCancelled() {
        return state == State.CANCELLED;
    }

    private boolean isPaused() {
        return state == State.PAUSED;
    }

    private <E> void requestDone(RequestSession requestSession, E data, boolean noCheckForPending) {

        if (isIdle() || isCompleted() || isCancelled()) {
            return;
        }

        int requestId = requestSession.getId();

        Request request = null;
        if (requestSession != null) {
            request = requestSession.getRequest();
        }

        if (request == null) {
            throw new IllegalArgumentException("Please make sure you are providing the correct request id, the request id you provided is:" + requestId
                    + ". Also, are you sure you are calling requestDone or Request.done with the right instance of Reqs object?");
        }

        if (data != null && request !=null && request.getExpectResponseType() != null
                && !request.getExpectResponseType().isInstance(data)) {
            requestFailed(requestSession, data);
            return;
        }

        Response response = new Response(data, requestSession, this);

        if (calling) {
            pendingRequestDone.add(response);
            return;
        }

        if (state == State.PAUSED) {
            pendingRequestLists.add(requestSession);
        } else {
            requestSession.next(response);
        }



        currentStepResponses.put(requestId, response);
        responses.put(requestId, response);
        SparseArray<RequestSession> map = requestLists.get(0).list;

        map.remove(requestId);
        if (map.size() == 0) {
            lastStepResponses = currentStepResponses;
            currentStepResponses = new SparseArray<>();
            if (requestLists.get(0).onNextListener != null) {
                RequestsMap firstMap = requestLists.get(0);
                if (isPaused()) {
                    pendingNext = firstMap.onNextListener;
                } else {
                    firstMap.onNextListener.onNext(this, lastStepResponses.getObjects());
                }
            }
            requestLists.remove(0);
            if (requestLists.size() > 0) {
                if (isPaused()) {
                    pendingPerformCurrent = true;
                } else {
                    performCurrent(noCheckForPending);
                }
            } else {
                if (isPaused()) {
                    pendingCompletion = true;
                } else {
                    complete();
                }
            }
        }
    }

    public synchronized void requestFailed(RequestSession requestSession, Object data) {

        if (isIdle() || isCompleted() || isCancelled()) {
            return;
        }

        int requestId = requestSession.getId();

        if (requestId == -1) {
            throw new IllegalArgumentException("Invalid requestId. Are you sure you are calling requestFailed or Request.fail with the right instance of Reqs object?");
        }

        if (calling) {
            if (pendingRequestFail == null) {
                pendingRequestFail = new Response(data, requestSession, this);
            }
            return;
        }

        responses.put(requestId, new Response(data, requestSession, this));
        failedRequestId = requestId;

        if (!failed) {
            failed = true;
            lastStepResponses = currentStepResponses;
            if (isPaused()) {
                pendingCompletion = true;
            } else {
                complete();
            }
        }
    }

    /**
     * Return the Response objects in last step
     * @return The Response objects in last step
     */
    public List<Response> getResponseList() {
        final List<Response> responseList = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            responseList.add(responses.get(responses.keyAt(i)));
        }
        return responseList;
    }

    private List<Response> getResponseList(int[] keys) {
        final List<Response> responseList = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            int key = responses.keyAt(i);
            for (int k : keys) {
                if (key == k) {
                    responseList.add(responses.get(key));
                }
            }
        }
        return responseList;
    }

    private void complete() {
        log("complete");
        state = State.COMPLETED;
        if (onDoneListener != null) {
            if (!failed) {
                final List<Response> responseList = getResponseList();
                onDoneListener.onSuccess(Reqs.this, responseList);
            } else {
                onDoneListener.onFailure(getResponseById(failedRequestId));
            }
        }

    }

    /**
     * Return <tt>true</tt> if the flow is started, false otherwise.
     * @return <tt>true</tt> if the flow is started, false otherwise.
     */
    public boolean isIdle() {
        return state == State.IDLE;
    }

    /**
     * Cancel the flow. Please note that the flow cannot be started again after this. Every Reqs can only start once.
     */
    public synchronized void cancel() {
        if (!isIdle() && !isCompleted() && !isCancelled()) {
            log("cancel");
            state = State.CANCELLED;
            if (onCancelListener != null) {
                onCancelListener.onCancel(this);
            }
        }
    }

    /**
     * Define what to do when the flow is cancelled
     * @param onCancelListener
     */
    public void setOnCancelListener(OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
    }

    /**
     * Define what to do when the flow is paused
     * @param onPauseListener
     */
    public void setOnPauseListener(OnPauseListener onPauseListener) {
        this.onPauseListener = onPauseListener;
    }

    /**
     * Define what to do when the flow is resumed
     * @param onResumeListener
     */
    public void setOnResumeListener(OnResumeListener onResumeListener) {
        this.onResumeListener = onResumeListener;
    }

    /**
     * Pause the flow. The responded data will cached and no more requests will be executed until resume() is called.
     * @see #resume()
     */
    public synchronized void pause() {
        if (state == State.REQUESTING) {
            log("pause");
            state = State.PAUSED;
            if (onPauseListener != null) {
                onPauseListener.onPause(this);
            }
        }
    }

    /**
     * Resume the flow. The cached response data will be process and the requests in the next step(if there is any) will be executed.
     */
    public synchronized void resume() {
        if (state == State.PAUSED) {
            log("resume");

            state = State.REQUESTING;

            if (onResumeListener != null) {
                onResumeListener.onResume(this);
            }

            for (RequestSession requestSession : pendingRequestLists) {
                requestSession.resume();
            }
            pendingRequestLists.clear();

            if (pendingNext != null) {
                pendingNext.onNext(this, lastStepResponses.getObjects());
                pendingNext = null;
            }

            if (pendingCompletion) {
                complete();
                pendingCompletion = false;
            }

            if (requestLists.size() > 0 && pendingPerformCurrent) {
                pendingPerformCurrent = false;
                performCurrent(false);
            }
        }
    }

    void log(String message) {
        Debugger.log("REQS:>" + message);
    }

    public Response getResponseById(int requestId) {
        return responses.get(requestId);
    }

    /**
     * Return the first object in the list of response that match the Class c.
     * @param c the class object of the data to be matched
     * @param <E> the class of the data to be matched
     * @return The first object in the list of response that match the Class c or null otherwise.
     */
    @SuppressWarnings("unchecked")
    public <E> E getData(Class<E> c) {

        E data = getLastStepData(c);
        if (data == null) {
            for (int i = 0; i < responses.size(); i++) {
                Response response = responses.get(responses.keyAt(i));
                if (c.isInstance(response.getData())) {
                    return (E) response.getData();
                }
            }
        }
        return data;
    }

    /**
     * Return the list of objects in the list of response that match the Class c.
     * @param c the class object of the data to be matched
     * @param <E> the class of the data to be matched
     * @return The list of objects in the list of response that match the Class c or null otherwise.
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> getDataList(Class<E> c) {
        List<E> list = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            Response response = responses.get(responses.keyAt(i));
            if (c.isInstance(response.getData())) {
                list.add((E)response.getData());
            } else if (response.getData() instanceof List) {
                List list1 = (List)response.getData();
                for (Object obj : list1) {
                    if (c.isInstance(obj)) {
                        list.add((E) obj);
                    }
                }
            }
        }
        return list;
    }

    /**
     * get the data of
     * @param c
     * @param <E>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <E> E getLastStepData(Class<E> c) {
        if (lastStepResponses.size() > 0) {
            for (int i = 0; i < lastStepResponses.size(); i++) {
                if (c.isInstance(lastStepResponses.getObjects().get(i).getData())) {
                    return (E) lastStepResponses.getObjects().get(i).getData();
                }
            }
        }
        return null;
    }

    public Reqs getLastStepReqs() {
        return getLastStepData(Reqs.class);
    }

    public List<Reqs> getLastStepReqsList() {
        return getLastStepDataList(Reqs.class);
    }

    /**
     * return the data of last step.
     * @param lastStepDataType the class of data response of last step
     * @param <DataType> the class of data response of last step
     * @return the data response of last step
     */
    public <DataType> DataType getLastStepReqsData(Class<DataType> lastStepDataType) {
        Reqs reqs = getLastStepReqs();
        if (reqs != null) {
            return reqs.getData(lastStepDataType);
        }
        return null;
    }

    public <DataType> List<DataType> getLastStepReqsDataList(Class<DataType> c) {
        Reqs reqs = getLastStepReqs();
        if (reqs != null) {
            return reqs.getDataList(c);
        }
        return Collections.emptyList();
    }

    /**
     * return all the responses
     * @return all the responses
     */
    public SparseArray<Response> getAllResponses() {
        return responses;
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> getLastStepDataList(Class<E> c) {
        List<E> list = new ArrayList<>();

        for (int i = 0; i < lastStepResponses.size(); i++) {
            Response response = lastStepResponses.getObjects().get(i);
            if (c.isInstance(response.getData())) {
                list.add((E) response.getData());
            } else if (response.getData() instanceof List) {
                List list1 = (List)response.getData();
                for (Object obj : list1) {
                    if (c.isInstance(obj)) {
                        list.add((E) obj);
                    }
                }
            }
        }

        return list;
    }

    private Response getLastStepResponse() {
        if (lastStepResponses.size() > 0) {
            return lastStepResponses.getObjects().get(0);
        } else {
            return null;
        }
    }

    private List<Response> getLastStepResponseList() {
        List<Response> list = new ArrayList<>();

        for (int i = 0; i < lastStepResponses.size(); i++) {
            Response response = lastStepResponses.getObjects().get(i);
            list.add(response);
        }

        return list;
    }

    static class RequestsMap {
        SparseArray<RequestSession> list = new SparseArray<>();
        OnNextListener onNextListener;
        int[] keys;

        public RequestsMap(int baseId, Reqs reqs, Request... requests) {
            int  id = baseId;
            keys = new int[requests.length];
            int i = 0;
            for (Request request : requests) {
                RequestSession requestSession = new RequestSession(id, reqs, request);
                list.put(id, requestSession);
                keys[i] = id;
                id++;
                i++;
            }
        }

        public List<Response> getResponses(Reqs reqs) {
            List<Response> resps = new ArrayList<>();
            for (int i = 0; i < reqs.responses.size(); i++) {
                Response response = reqs.responses.get(reqs.responses.keyAt(i));
                if (keys[i] == response.getRequestSession().getId()) {
                    resps.add(response);
                }
            }

            return resps;
        }
    }

    /**
     * Define the conditions to perform different request.
     */
    public interface OnSwitchListener {
        /**
         * Define the conditions to perform different request.
         * @param reqs
         * @return a request that define the conditions to perform different request. proceed to next request if return null.
         */
        Request onSwitch(Reqs reqs);
    }

    /**
     * Define what to do in cancelling the flow
     * @see #cancel()
     */
    public interface OnCancelListener {
        /**
         * Define what to do in pausing the flow
         * @see #pause()
         * @param reqs current Reqs instance
         */
        void onCancel(Reqs reqs);
    }

    /**
     * Define what to do in pausing the flow
     * @see #pause()
     */
    public interface OnPauseListener {
        /**
         * Define what to do in pausing the flow
         * @see #pause()
         * @param reqs current Reqs instance
         */
        void onPause(Reqs reqs);
    }

    /**
     * Define what to do in resuming the flow
     * @see #resume()
     */
    public interface OnResumeListener {
        /**
         * Define what to do in resuming the flow
         * @see #resume()
         * @param reqs current Reqs instance
         */
        void onResume(Reqs reqs);
    }

    /**
     * Define what to do next when the requests in the current step is finished
     * @see #next(OnNextListener)
     */
    public interface OnNextListener {
        /**
         * Define what to do next when the requests in the current step is finished
         * @see #next(OnNextListener)
         * @param reqs current Reqs instance
         * @param responses all responses data fetched in last step
         */
        void onNext(Reqs reqs, List<Response> responses);
    }

    /**
     * Define what to do when the flow is done successfully or failed.
     */
    public interface OnDoneListener {
        /**
         * Define what to do when the flow is done successfully and all the requests responded by calling session.done(responseData).
         * @param reqs
         * @param responses
         */
        void onSuccess(Reqs reqs, List<Response> responses);

        /**
         * Define what to do when the flow is failed. This is called when any session.fail(errorData) is called.
         * @param failedResponse
         */
        void onFailure(Response failedResponse);
    }

    public OnDoneListener getOnDoneListener() {
        return onDoneListener;
    }
}
