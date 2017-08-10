package msg;

import org.json.JSONObject;

/**
 * Created by yizhe on 17-7-20.
 */
public class MatchingCancelResMsg extends Msg {
    public MatchingCancelResMsg() {
        super("matching_cancel_res");
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);

        return object.toString();
    }
}
