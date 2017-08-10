package msg;

import org.json.JSONObject;

/**
 * Created by yizhe on 17-7-19.
 */
public class EventStatMsg extends Msg {
    private boolean valid;
    private int eventID; //0 lucky card 1 lord seal 2 monkey shield 3 tangMonk frozen

    public EventStatMsg(boolean valid, int eventID) {
        super("event_stat");
        this.valid = valid;
        this.eventID = eventID;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("valid", valid);
        object.put("event_id", eventID);

        return object.toString();
    }
}
