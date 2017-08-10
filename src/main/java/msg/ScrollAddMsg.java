package msg;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by yizhe on 17-8-4.
 */
public class ScrollAddMsg extends Msg {
    private ArrayList<Integer> card;
    private int position;
    private int index;

    public ScrollAddMsg(ArrayList<Integer> card, int position, int index) {
        super("scroll_add");
        this.card = card;
        this.position = position;
        this.index = index;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("card", card);
        object.put("position", position);
        object.put("index", index);

        return object.toString();
    }
}
