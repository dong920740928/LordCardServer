package msg;

import org.json.JSONObject;

/**
 * Created by yizhe on 17-8-2.
 */
public class ChooseRoleMsg extends Msg {
    public int index;
    public String uuid;
    //public int role;

    public ChooseRoleMsg(String msg) {
        super("poker_play");
        JSONObject object = new JSONObject(msg);
        index = object.getInt("index");
        uuid = object.getString("uuid");
        //role = object.getInt("role");
    }
}
