package msg;

import org.json.JSONObject;

/**
 * Created by dong on 13/07/2017.
 */
public class GameOverMsg extends Msg {
    private int[] totalDamage;
    private int[] playedLucky;
    private int[] playedSkill;
    private int[] playedBoom;
    private int end;

    public GameOverMsg(int [] totalDamage, int [] playedLucky, int[] playedSkill, int[] playedBoom, int end) {
        super("game_over");
        this.totalDamage = totalDamage;
        this.playedLucky = playedLucky;
        this.playedSkill = playedSkill;
        this.playedBoom = playedBoom;
        this.end = end;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("total_damage", totalDamage);
        object.put("played_lucky", playedLucky);
        object.put("played_skill", playedSkill);
        object.put("played_boom", playedBoom);
        object.put("end", end);

        return object.toString();
    }
}
