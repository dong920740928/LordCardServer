package msg;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by yizhe on 17-7-19.
 */
public class StartPokersMsg extends Msg {
    private List<Integer> startPokers;
    private int[] roles;
    private int[] initialHP;

    public StartPokersMsg(List<Integer> startPokers, int[] roles, int[] initialHP) {
        super("start_pokers");
        this.startPokers = startPokers;
        this.roles = roles;
        this.initialHP = initialHP;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("start_pokers", startPokers);
        object.put("roles", roles);
        object.put("initial_hp", initialHP);

        return object.toString();
    }
}
