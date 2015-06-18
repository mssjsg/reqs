package reqs;

import reqs.Reqs;
import reqs.RequestSession;

/**
 * Created by maksing on 14/6/15.
 * A wrapper for the data response
 * @param <E> the expected data type
 */
public class Response<E> {
    private final E data;
    private final RequestSession<E> requestSession;
    private final Reqs reqs;

    /**
     * Constructor
     * @param response data response
     * @param requestSession the request session of this response
     * @param reqs the current Reqs instance
     */
    public Response(E response, RequestSession<E> requestSession, Reqs reqs) {
        this.data = response;
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
        if (c.isInstance(data)) {
            return (T)data;
        }
        return null;
    }

    /**
     * Return response data
     * @return response data
     */
    public E getData() {
        return data;
    }

    /**
     * return the request session
     * @return the request session
     */
    public RequestSession<E> getRequestSession() {
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