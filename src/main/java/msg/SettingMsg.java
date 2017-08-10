package msg;

import org.json.JSONObject;

/**
 * Created by yizhe on 17-8-7.
 */
public class SettingMsg extends Msg {
    private String settings;

    public SettingMsg(String settings) {
        super("settings");
        this.settings = settings;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("settings", settings);

        return object.toString();
    }
}
