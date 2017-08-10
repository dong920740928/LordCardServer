package msg;

import org.json.JSONObject;

/**
 * Created by dong on 13/07/2017.
 */
public class LoginResMsg extends Msg {
    private String uuid;
    private int res;

    /**
     *
     * @param uuid 用户ID
     * @param res 登录结果
     *            -1: 用户名重复;
     *            0: 用户被托管, 断线重连;
     *            1: 登录成功
     */
    public LoginResMsg(String uuid, int res) {
        super("login_res");
        this.uuid = uuid;
        this.res = res;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("res", res);
        object.put("uuid", uuid);

        return object.toString();
    }
}
