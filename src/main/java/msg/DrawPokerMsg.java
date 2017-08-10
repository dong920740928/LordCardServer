package msg;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by yizhe on 17-7-19.
 */
public class DrawPokerMsg extends Msg {
    private List<Integer> pokers;

    public DrawPokerMsg(List<Integer> pokers) {
        super("draw_poker");

        this.pokers = pokers;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("cards", pokers);

        return object.toString();
    }
}
