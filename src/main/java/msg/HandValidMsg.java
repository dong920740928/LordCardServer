package msg;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by yizhe on 17-7-19.
 */
public class HandValidMsg extends Msg {
    private boolean valid;
    private List<Integer> cards;
    private int powerAdd;
    private int power;
    private int resCode;

    public HandValidMsg(boolean valid, List<Integer> cards, int powerAdd, int power, int resCode) {
        super("hand_valid");
        this.cards = cards;
        this.valid = valid;
        this.powerAdd = powerAdd;
        this.power = power;
        this.resCode = resCode;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("valid", valid);
        object.put("cards", cards);
        object.put("power_add", powerAdd);
        object.put("power", power);
        object.put("res_code", resCode);

        return object.toString();
    }
}
