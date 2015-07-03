package reqs;

import java.util.List;

/**
 * Created by maksing on 14/6/15.
 */
public class ReqsRequest extends Request {

    private Reqs subReqs;

    public ReqsRequest(Reqs reqs) {
        this(reqs, true);
    }

    public ReqsRequest(Reqs reqs, boolean pausable) {
        super(Reqs.class);
        this.pausable = pausable;
        this.subReqs = Reqs.createWithReqs(reqs);
    }

    @Override
    public void onCall(final RequestSession requestSession) {
        final Reqs.OnDoneListener oldOnDoneListener = subReqs.getOnDoneListener();

        Reqs reqs = Reqs.createWithReqs(subReqs).done(new Reqs.OnDoneListener() {
            @Override
            public void onSuccess(Reqs reqs, List<Response> responses) {
                if (oldOnDoneListener != null) {
                    oldOnDoneListener.onSuccess(reqs, responses);
                }
                requestSession.done(reqs);
            }

            @Override
            public void onFailure(Response failedResponse) {
                if (oldOnDoneListener != null) {
                    oldOnDoneListener.onFailure(failedResponse);
                }
                requestSession.fail(failedResponse.getData());
            }
        });
        requestSession.setSubReqs(reqs);
        reqs.start();

    }
}
