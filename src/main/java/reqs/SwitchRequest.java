package reqs;

/**
 * Created by singmak on 16/6/15.
 */
public class SwitchRequest extends Request {

    private Reqs.OnSwitchListener onSwitchListener;

    public SwitchRequest(Reqs.OnSwitchListener onSwitchListener) {
        this.onSwitchListener = onSwitchListener;
    }

    @Override
    public void onCall(RequestSession requestSession) {
        Request request = onSwitchListener.onSwitch(requestSession.getReqs());
        if (request != null) {
            request.onCall(requestSession);
        } else {
            requestSession.done(null);
        }
    }
}
