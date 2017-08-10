package msg;

import model.Player;
import org.json.JSONObject;
import poker.Hand;

import java.util.List;
import java.util.Queue;

/**
 * Created by yizhe on 17-7-20.
 */
public class GameResumeMsg extends Msg {
    private List<Integer> handCards;
    private List<Integer> candidateCards;
    private String[] players;
    private int[] roles;
    private int[] initialHP;
    private int[] HP;
    private int lordSeal;
    private int luckyCard;
    private int luckyCardOwn;

    public GameResumeMsg(List<Integer> handCards, List<Integer> candidateCards, List<Player> players, int[] roles, int[] initialHP, int[] HP, int lordSeal, int luckyCard, int luckyCardOwn) {
        super("game_resume");
        this.handCards = handCards;
        this.candidateCards = candidateCards;
        this.players = new String[3];
        for (int i = 0; i < 3; ++i) {
            this.players[i] = players.get(i).getUuid();
        }
        this.roles = roles;
        this.initialHP = initialHP;
        this.HP = HP;
        this.lordSeal = lordSeal;
        this.luckyCard = luckyCard;
        this.luckyCardOwn = luckyCardOwn;
    }

    public String toString() {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("hand_cards", handCards);
        object.put("candidate_cards", candidateCards);
        object.put("players", players);
        object.put("roles", roles);
        object.put("initial_hp", initialHP);
        object.put("hp", HP);
        object.put("lord_seal", lordSeal);
        object.put("lucky_card", luckyCard);
        object.put("lucky_own", luckyCardOwn);

        return object.toString();
    }
}
