package msg;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yizhe on 17-7-22.
 */
public class PokerPlayMsg extends Msg {
    public String uuid;
    public int index;
    public ArrayList<Integer> pokersPlayed;
    public int clientDamage;

    public PokerPlayMsg(String msg) {
        super("poker_play");
        JSONObject object = new JSONObject(msg);
        this.uuid = object.getString("uuid");
        this.index = object.getInt("index");

        JSONArray cards = object.getJSONArray("pokers_played");
        this.pokersPlayed = new ArrayList<Integer>();
        int len = cards.length();
        for (int i = 0; i < len; ++i) {
            pokersPlayed.add(cards.getInt(i));
        }

        this.clientDamage = object.getInt("hp_selfcomputed");
    }
}
