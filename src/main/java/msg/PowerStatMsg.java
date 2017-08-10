package msg;

import org.json.JSONObject;

/**
 * Created by yizhe on 17-8-2.
 */
public class PowerStatMsg extends Msg {
    private float[] playersPowerPercent;

    public PowerStatMsg(int[] playersPower, int[] playersMaxPower) {
        super("power_stat");
        this.playersPowerPercent = new float[3];
        for (int i = 0; i < 3; ++i) {
            playersPowerPercent[i] = (float) (playersPower[i] * 1.0 / playersMaxPower[i]);
        }
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("players_power_percent", playersPowerPercent);

        return object.toString();
    }
}
