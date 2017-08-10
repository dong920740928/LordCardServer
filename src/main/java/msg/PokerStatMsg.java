package msg;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by yizhe on 17-7-22.
 */
public class PokerStatMsg extends Msg {
    private int index; //发动攻击的玩家
    private int end; //游戏进行状态
    private int[] playersHP; //玩家当前血量
    private List<Integer> last; //玩家打出的牌
    private int damage; // 这一手牌的伤害
    private int lucky;
    private int lordSeal;
    private int tangMonkFrozen;
    private int monkeyShield;

    public PokerStatMsg(int index, int end, int[] playersHP, List<Integer> last, int damage, int lucky, int lordSeal, int tangMonkFrozen, int monkeyShield) {
        super("poker_stat");
        this.index = index;
        this.end = end;
        this.playersHP = playersHP;
        this.last = last;
        this.damage = damage;
        this.lucky = lucky;
        this.lordSeal = lordSeal;
        this.tangMonkFrozen = tangMonkFrozen;
        this.monkeyShield = monkeyShield;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("index", index);
        object.put("end", end);
        object.put("players_hp", playersHP);
        object.put("last", last);
        object.put("damage", damage);
        object.put("lucky", lucky);
        object.put("lord_seal", lordSeal);
        object.put("monk_frozen", tangMonkFrozen);
        object.put("monkey_shield", monkeyShield);

        return object.toString();
    }
}
