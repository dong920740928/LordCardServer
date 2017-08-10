package msg;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by yizhe on 17-8-4.
 */
public class ScrollDelMsg extends Msg {
    private ArrayList<Integer> card;
    private int position;

    public ScrollDelMsg(ArrayList<Integer> card, int position) {
        super("scroll_del");
        this.card = card;
        this.position = position;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("card", card);
        object.put("position", position);

        return object.toString();
    }
}
