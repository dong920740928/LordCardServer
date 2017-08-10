package msg;

import org.json.JSONObject;

/**
 * Created by yizhe on 17-7-21.
 */
public class LuckyCardMsg extends Msg{
    private int source;
    private int target;
    private int card;
    private int timer;

    public LuckyCardMsg(int source, int target, int luckyCard, int timer) {
        super("lucky_card");
        this.source = source;
        this.target = target;
        this.card = luckyCard;
        this.timer = timer;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("source", source);
        object.put("target", target);
        object.put("card", card);
        object.put("timer", timer);

        return object.toString();
    }
}
