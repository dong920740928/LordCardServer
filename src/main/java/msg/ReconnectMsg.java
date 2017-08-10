package msg;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by dong on 13/07/2017.
 */
public class ReconnectMsg extends Msg {
    public ReconnectMsg() {
        super("reconnected");
    }

    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);

        return object.toString();
    }
}
