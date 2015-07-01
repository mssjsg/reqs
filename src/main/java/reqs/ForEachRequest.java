package reqs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maksing on 1/7/15.
 */
public abstract class ForEachRequest<E> extends Request {

    public abstract List<E> getParamsList(Reqs reqs);

    @Override
    public void onCall(final RequestSession requestSession) {
        List<E> list = getParamsList(requestSession.getReqs());

        List<Request> requests = new ArrayList<Request>();

        for (E param : list) {
            requests.add(getRequest(param));
        }

        Reqs.create().then(requests).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {
                requestSession.done(reqs);
            }

            @Override
            public void onFailure(Response failedResponse) {
                requestSession.fail(failedResponse.getData());
            }
        }).start();
    }

    public abstract Request getRequest(E param);
}
