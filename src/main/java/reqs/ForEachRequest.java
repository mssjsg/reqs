package reqs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maksing on 1/7/15.
 * Call a request with different param in a list.
 */
public abstract class ForEachRequest<E> extends Request {

    /**
     * Return the parameters list
     * @param reqs
     * @return the parameters list
     */
    public abstract List<E> getParamsList(Reqs reqs);

    @Override
    public void onCall(final RequestSession requestSession) {
        List<E> list = getParamsList(requestSession.getReqs());

        List<Request> requests = new ArrayList<Request>();

        for (E param : list) {
            requests.add(getRequest(param));
        }

        Reqs reqs = Reqs.create().then(requests).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {
                requestSession.done(reqs);
            }

            @Override
            public void onFailure(Response failedResponse) {
                requestSession.fail(failedResponse.getData());
            }
        }).setOnPauseListener(new Reqs.OnPauseListener() {
            @Override
            public void onPause(Reqs reqs) {

            }
        });
        requestSession.setSubReqs(reqs);
        reqs.start();
    }

    /**
     * apply a parameter from the list returned from getParamsList to a request and return the request
     * @param param
     * @return a request with a parameter in the list applied
     */
    public abstract Request getRequest(E param);
}
