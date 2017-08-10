package msg;

import model.Player;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yizhe on 17-7-18.
 */
public class GameStartMsg extends Msg {
    private String[] playersUuid;
    private int[] playersRole;

    public GameStartMsg (List<Player> players, int[] playersRole) {
        super("game_start");
        playersUuid = new String[3];
        for (int i = 0; i < 3; ++i) {
            playersUuid[i] = players.get(i).getUuid();
        }
        this.playersRole = playersRole;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("players_uuid", playersUuid);
        object.put("players_role", playersRole);

        return object.toString();
    }

}
