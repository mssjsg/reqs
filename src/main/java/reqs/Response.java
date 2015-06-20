package reqs;

import reqs.Reqs;
import reqs.RequestSession;

/**
 * Created by maksing on 14/6/15.
 * A wrapper for the data response
 */
public class Response {
    private final Object data;
    private final RequestSession requestSession;
    private final Reqs reqs;

    /**
     * Constructor
     * @param data data response
     * @param requestSession the request session of this response
     * @param reqs the current Reqs instance
     */
    public Response(Object data, RequestSession requestSession, Reqs reqs) {
        this.data = data;
        this.requestSession = requestSession;
        this.reqs = reqs;
    }

    /**
     * return the data if the data match class c
     * @param c the class object type to match
     * @param <T> class type
     * @return The data if the data match class c
     */
    public <T> T getData(Class<T> c) {
        try {
            return c.cast(data);
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Return response data
     * @return response data
     */
    public Object getData() {
        return data;
    }

    /**
     * return the request session
     * @return the request session
     */
    public RequestSession getRequestSession() {
        return requestSession;
    }

    /**
     * Return current Reqs instance
     * @return current Reqs instance
     */
    public Reqs getReqs() {
        return reqs;
    }
}