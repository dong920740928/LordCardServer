package msg;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by yizhe on 17-7-25.
 */
public class SupplyPokerMsg extends Msg {
    private List<Integer> cards;

    public SupplyPokerMsg(List<Integer> cards) {
        super("supply_poker");
        this.cards = cards;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("cards", cards);

        return object.toString();
    }
}
