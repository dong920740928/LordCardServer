package msg;

import org.json.JSONObject;

/**
 * Created by yizhe on 17-7-17.
 */
public class KeepAliveMsg extends Msg {
    public KeepAliveMsg() {
        super("keep_alive");
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);

        return object.toString();
    }
}
