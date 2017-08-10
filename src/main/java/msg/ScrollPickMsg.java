package msg;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by yizhe on 17-8-4.
 */
public class ScrollPickMsg extends Msg {
    private ArrayList<ArrayList<Integer>> cards;
    private ArrayList<Integer> positions;
    public int damage;

    public ScrollPickMsg(ArrayList<ArrayList<Integer>> cards, ArrayList<Integer> positions, int damage) {
        super("scroll_pick");
        this.cards = cards;
        this.positions = positions;
        this.damage = damage;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("cards", cards);
        object.put("positions", positions);
        object.put("damage", damage);

        return object.toString();
    }
}
