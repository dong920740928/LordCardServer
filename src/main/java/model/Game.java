package model;

import msg.ChooseRoleMsg;
import msg.GameOverMsg;
import msg.GameResumeMsg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import poker.Hand;
import poker.Poker;
import server.MainServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by dong on 9/07/2017.
 */
public class Game {
    private static final Logger LOGGER = LogManager.getLogger(Game.class);
    private static final String CONFIG_PATH = "/home/yizhe/workplace/LordCardSettings/settings.json";

    public enum RoleID {
        BULLDEMON,
        MONKEY,
        TANGMONK,
    }

    public enum GameState {
        READY,
        PLAYING,
        PAUSED,
        END,
    }

    public enum EventID {
        LUCKY,
        SEAL,
        SHIELD,
        FROZEN,
    }

    public enum HandResCode {
        SUCCESS,
        CARDS_LACK,
        TYPE_INVALID,
        FROZEN,
        COOL_DOWN,
        BREAK,
    }

    //game state
    public List<Player> players;
    public int pause;
    public GameState state;
    public Long startTime;

    //game core logic
    public int[] playersRole; //Roles of players
    public int[] playersHP; //HP of players
    public PokerPool[] pokerPools; //pokers in pool of players
    public ArrayList<Integer>[] pokerHands; //pokers in hand
    public LinkedList<Integer>[] candidateCards; // pokers candidate
    public int[] playerCardsNum; //max num of hand pokers of player
    public Long[] playCardTime;
    public Long[] drawCardTime; //last draw card time
    public int[] playerDrawCardCD; //CD of drawing card

    //role source
    public int[] playersPower;
    public int[] playersMaxPower;

    //RP
    public float[] playerDrawCardRP;

    //skill state
    public int lordSeal; //牛魔王封印层数
    public int skillDelayDoing;
    public int tangMonkFrozen;
    public Long frozenTime;
    public int monkeyShield;
    public Long shieldTime;
    public Long skillDelaytime;

    //lucky card
    public int luckyCard; //val of lucky card
    public int luckyCardOwn; //owner of lucky card
    public Long luckyTime; //lucky card active time

    public int lord;
    public int monkey;
    public int tangMonk;

    //game statistics
    public int[] totalDamage;
    public int end;
    public int[] playedLucky;
    public int[] playedSkill;
    public int[] playedBoom;

    public Hand lordRecentHand;
    public Hand mentorRecentHand;
    public int mentorRecentIndex;

    //scroll hand bar
    public ScrollHandBar lordBar;
    public ScrollHandBar mentorBar;
    public Long lordBarAddTime;
    public Long mentorBarAddTime;

    //game arguments
    public static int[] roleHP;
    public static int[] roleCardsNum;
    public static int[] roleMaxPower;
    public static int playCardCD;
    public static int drawCardCD;
    public static int luckyCardLive;
    public static float luckyChance;
    public static int candidateNum;
    public static float luckyDamageFactor;
    public static float monkeyStickDamageFactor;
    public static int monkeyStickDamageBase;

    public static int startGameDelay;
    public static int skillDelay;
    public static int reconnectDelay;
    public static int chooseRolesDelay;

    public static int scrollDuration;
    public static int scrollAddCD;

    public static float drawCardRP;
    public static int tangMonkFrozenDuration;

    public static String settingsStr;
    private static Random random;

    public static int setGameArgs() {
        random = new Random(System.currentTimeMillis());
        settingsStr = null;
        File file = new File(CONFIG_PATH);
        try {
            BufferedReader input = new BufferedReader(new FileReader(file));
            settingsStr = input.readLine();
            input.close();
        } catch (Exception e) {
            LOGGER.error("read setting error: " + e.getMessage());
            return -1;
        }

        if (settingsStr != null) {
            JSONObject object = new JSONObject(settingsStr);
            roleHP = new int[3];
            roleHP[RoleID.BULLDEMON.ordinal()] = object.getInt("lordHP");
            roleHP[RoleID.MONKEY.ordinal()] = object.getInt("monkeyHP");
            roleHP[RoleID.TANGMONK.ordinal()] = object.getInt("tangMonkHP");
            roleCardsNum = new int[3];
            roleCardsNum[RoleID.BULLDEMON.ordinal()] = object.getInt("lordCardsNum");
            roleCardsNum[RoleID.MONKEY.ordinal()] = object.getInt("monkeyCardsNum");
            roleCardsNum[RoleID.TANGMONK.ordinal()] = object.getInt("tangMonkCardsNum");
            roleMaxPower = new int[3];
            roleMaxPower[RoleID.BULLDEMON.ordinal()] = object.getInt("lordMaxPower");
            roleMaxPower[RoleID.MONKEY.ordinal()] = object.getInt("monkeyMaxPower");
            roleMaxPower[RoleID.TANGMONK.ordinal()] = object.getInt("tangMonkMaxPower");
            playCardCD = object.getInt("playCardCD");
            drawCardCD = object.getInt("drawCardCD");
            luckyCardLive = object.getInt("luckyCardLive");
            luckyChance = (float)object.getDouble("luckyChance");
            candidateNum = object.getInt("candidateNum");
            luckyDamageFactor = (float)object.getDouble("luckyDamageFactor");
            monkeyStickDamageFactor = (float)object.getDouble("monkeyStickDamageFactor");
            monkeyStickDamageBase = object.getInt("monkeyStickDamageBase");
            startGameDelay = object.getInt("startGameDelay");
            skillDelay = object.getInt("skillDelay");
            reconnectDelay = object.getInt("reconnectDelay");
            chooseRolesDelay = object.getInt("chooseRolesDelay");
            scrollDuration = object.getInt("scrollDuration");
            scrollAddCD = object.getInt("scrollAddCD");
            drawCardRP = (float)object.getDouble("drawCardRP");
            tangMonkFrozenDuration = object.getInt("tangMonkFrozenDuration");
        } else {
            roleHP = new int[]{1200, 600, 600};
            roleCardsNum = new int[]{14, 12, 12};
            roleMaxPower = new int[]{6, 8, 4};
            playCardCD = 1500;
            drawCardCD = 1500;
            luckyCardLive = 10000;
            luckyChance = 1;
            candidateNum = 6;
            luckyDamageFactor = 2;
            monkeyStickDamageFactor = 10;
            monkeyStickDamageBase = 30;
            startGameDelay = 6500;
            skillDelay = 4000;
            reconnectDelay = 30;
            chooseRolesDelay = 5000;
            scrollDuration = 10000;
            scrollAddCD = 3500;
            drawCardRP = 0;
            tangMonkFrozenDuration = 20000;
        }


        for (int i = 0; i < 3; ++i) {
            LOGGER.info(roleHP[i]);
            LOGGER.info(roleCardsNum[i]);
            LOGGER.info(roleMaxPower[i]);
        }

        LOGGER.info(playCardCD);
        LOGGER.info(drawCardCD);
        LOGGER.info(luckyCardLive);
        LOGGER.info(luckyChance);
        LOGGER.info(candidateNum);
        LOGGER.info(luckyDamageFactor);
        LOGGER.info(monkeyStickDamageFactor);
        LOGGER.info(monkeyStickDamageBase);

        LOGGER.info(startGameDelay);
        LOGGER.info(skillDelay);
        LOGGER.info(reconnectDelay);
        LOGGER.info(chooseRolesDelay);

        LOGGER.info(scrollDuration);
        LOGGER.info(scrollAddCD);

        LOGGER.info(drawCardRP);
        LOGGER.info(tangMonkFrozenDuration);

        return 0;
    }

    //delay
    public static boolean delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            LOGGER.error("delay error: " + e.getMessage());
            return false;
        }

        return true;
    }

    //decide lucky card active or not
    public static boolean isLucky() {
        Random random = new Random(System.currentTimeMillis());
        float r = random.nextFloat();

        return r < luckyChance;
    }

    //choose a lucky card from a hand
    public static Integer chooselucky(List<Integer> cards) {
        Random random = new Random(System.currentTimeMillis());
        int r = random.nextInt(cards.size());
        int card = cards.get(r);
        int lucky = (card & 0xff0) + 0x005;

        return lucky;
    }

    public static boolean hasLucky(List<Integer> cards) {
        for (int i = 0; i < cards.size(); ++i) {
            if (new Poker(cards.get(i)).pattern == 5) {
                return true;
            }
        }

        return false;
    }

    public static int[] randPerm(int n) {
        int []res = new int[n];
        ArrayList<Integer> arr = new ArrayList<Integer>();
        for (int i = 0; i < n; ++i) {
            arr.add(i);
        }
        for (int i = 0; i < n; ++i) {
            int r = random.nextInt(arr.size());
            res[i] = arr.remove(r);
        }

        return res;
    }

    public Game(List<Player> players) {
        this.players = players;
        this.pause = 0;
        this.state = GameState.READY;
        this.startTime = System.currentTimeMillis();
    }

    //判断玩家出牌是否成功
    public HandResCode playSuccess(int index, Hand hand, Long time) {
        HandResCode handResCode = HandResCode.SUCCESS;

        if (hand.type == Hand.Type.INVALID) {
            handResCode = Game.HandResCode.TYPE_INVALID;
        }

        if (!pokerHands[index].containsAll(hand.cards)) {
            handResCode = Game.HandResCode.CARDS_LACK;
        }

        //play card CD check
        if (time < playCardTime[index] + Game.playCardCD) {
            handResCode = Game.HandResCode.COOL_DOWN;
        }

        //lord can not send spade during frozen
        if (tangMonkFrozen == 1 && index == lord) {
            if (hand.patternNum(Hand.Pattern.SPADE.ordinal()) > 0) {
                handResCode = Game.HandResCode.FROZEN;
            }
        }

        if (skillDelayDoing == 1) {
            handResCode = HandResCode.BREAK;
        }

        return handResCode;
    }

    //从玩家的补牌队列向手牌中补牌, 返回所补的牌
    public List<Integer> supplyCards(int index) {
        int num = Math.min(playerCardsNum[index] - pokerHands[index].size(), candidateCards[index].size());
        List<Integer> supplyCards = new ArrayList<Integer>();
        for (int i = 0; i < num; ++i) {
            supplyCards.add(candidateCards[index].poll());
        }

        return supplyCards;
    }

    //选择角色
    public void chooseRole() {
        playersRole = Game.randPerm(3);
        for (int i = 0; i < 3; ++i) {
            if (playersRole[i] == RoleID.BULLDEMON.ordinal()) {
                lord = i;
            } else if (playersRole[i] == RoleID.MONKEY.ordinal()) {
                monkey = i;
            } else if (playersRole[i] == RoleID.TANGMONK.ordinal()) {
                tangMonk = i;
            }
        }

//        lord = random.nextInt(3);
//
//        if (lord == 0) {
//            monkey = 1;
//        } else {
//            monkey = 0;
//        }
//        tangMonk = 3 - lord - monkey;
//
//        playersRole = new int[3];
//        playersRole[monkey] = Game.RoleID.MONKEY.ordinal();
//        playersRole[tangMonk] = Game.RoleID.TANGMONK.ordinal();
//        playersRole[lord] = Game.RoleID.BULLDEMON.ordinal();
    }

    //初始化本局游戏参数
    public void initGame() {
        //初始化玩家手牌上限
        playerCardsNum = new int[3];
        for (int i = 0; i < 3; ++i) {
            playerCardsNum[i] = Game.roleCardsNum[playersRole[i]];
        }

        //初始化玩家HP
        playersHP = new int[3];
        for (int i = 0; i < 3; ++i) {
            playersHP[i] = Game.roleHP[playersRole[i]];
        }

        //初始化玩家牌库
        pokerPools = new PokerPool[3];
        for (int i = 0; i < 3; ++i) {
            if (players.get(i).getUuid().equals("saber")) {
                pokerPools[i] = new PokerPool(i, 1, this);
            } else {
                pokerPools[i] = new PokerPool(i, 0, this);
            }

        }

        //重置发牌时间
        drawCardTime = new Long[3];
        for (int i = 0; i < 3; ++i) {
            drawCardTime[i] = System.currentTimeMillis();
        }

        //reset play card time
        playCardTime = new Long[3];
        for (int i = 0; i < 3; ++i) {
            playCardTime[i] = 0L;
        }

        //抽牌cd初始化
        this.playerDrawCardCD = new int[3];
        for (int i = 0; i < 3; ++i) {
            playerDrawCardCD[i] = drawCardCD;
        }

        //RP
        this.playerDrawCardRP = new float[3];
        for (int i = 0; i < 3; ++i) {
            playerDrawCardRP[i] = drawCardRP;
        }

        //player power init
        this.playersPower = new int[3];
        for (int i = 0; i < 3; ++i) {
            playersPower[i] = 0;
        }

        //player max power
        this.playersMaxPower = new int[3];
        for (int i = 0; i < 3; ++i) {
            playersMaxPower[i] = roleMaxPower[playersRole[i]];
        }

        //初始化玩家候选牌
        candidateCards = new LinkedList[3];
        for (int i = 0; i < 3; ++i) {
            candidateCards[i] = new LinkedList<Integer>();
        }

        //初始化玩家手牌
        pokerHands = new ArrayList[3];
        for (int i = 0; i < 3; ++i) {
            pokerHands[i] = new ArrayList<Integer>();
//            for (int j = 0; j < playerCardsNum[i]; ++j) {
//                pokerHands[i].add(pokerPools[i].getPoker());
//            }
            for (int j = 0; j < playerCardsNum[i] / 2; ++j) {
                pokerHands[i].add(pokerPools[i].getPoker());
            }
            for (int j = playerCardsNum[i] / 2; j < playerCardsNum[i]; ++j) {
                pokerHands[i].add(drawPoker(i));
            }
        }



        //牛魔王封印层数
        this.lordSeal = -1;

        //技能释放延时
        this.skillDelayDoing = -1;
        this.skillDelaytime = 0L;

        //tangMonk frozen init
        this.tangMonkFrozen = -1;
        this.frozenTime = 0L;

        //monkey shield init
        this.monkeyShield = -1;
        this.shieldTime = 0L;

        //重置伤害统计
        this.totalDamage = new int[3];
        for (int i = 0; i < 3; ++i) {
            totalDamage[i] = 0;
        }

        //重置lucky card统计
        this.playedLucky = new int[3];
        for (int i = 0; i < 3; ++i) {
            playedLucky[i] = 0;
        }

        //重置技能发动统计
        this.playedSkill = new int[3];
        for (int i = 0; i < 3; ++i) {
            playedSkill[i] = 0;
        }

        //重置炸弹统计
        this.playedBoom = new int[3];
        for (int i = 0; i < 3; ++i) {
            playedBoom[i] = 0;
        }

        //scroll bar init
        this.lordBar = new ScrollHandBar();
        this.mentorBar = new ScrollHandBar();
        this.lordBarAddTime = 0L;
        this.mentorBarAddTime = 0L;
        this.lordRecentHand = null;
        this.mentorRecentHand = null;
        this.mentorRecentIndex = -1;
    }

    //能量积攒判定
    public int getPower(int index, Hand hand) {
        int powerAdd = 0;
        if (index == lord) {
            //牛魔王能量判定
            powerAdd += hand.patternNum(Hand.Pattern.SPADE.ordinal());
        } else if (index == monkey) {
            //monkey get power
            powerAdd += hand.doubleNum();
        } else if (index == tangMonk) {
            //tangMonk get power
            powerAdd += hand.tripleNum();
        }

        return powerAdd;
    }

    //draw poker from pool
    public int drawPoker(int index) {
        int poker;
        Random random = new Random(System.currentTimeMillis());
        float r = random.nextFloat();
        float RP = playerDrawCardRP[index];
        if (pokerHands[index].size() <= 4) {
            RP *= 0.4;
        } else if (pokerHands[index].size() <= 8) {
            RP *= 0.7;
        }

        if (r < RP) {
            //lucky
            poker = pokerPools[index].getValuePoker(index);
            if (poker == -1) {
                poker = pokerPools[index].getPoker();
            }
        } else {
            poker = pokerPools[index].getPoker();
        }

        return poker;
    }

    //poker value calculate
    public int pokerValue(int index, int pokerID) {
        if (pokerID == 0x104 || pokerID == 0x114) {
            return 1;
        }

        List<Integer> list = new ArrayList<Integer>();
        list.addAll(pokerHands[index]);
        list.addAll(candidateCards[index]);
        list.add(pokerID);

        Poker poker = new Poker(pokerID);

        Map<Integer, Integer> map = Hand.getMap(list);
        if (map.get(poker.figure) >= 2) {
            return 1;
        }

        int down = 0;
        for (int figure = poker.figure - 1; figure >= Math.max(0x03, poker.figure - 4); --figure) {
            if (map.get(figure) > 0) {
                ++down;
            } else {
                break;
            }
        }

        int up = 0;
        for (int figure = poker.figure + 1; figure <= Math.min(0x0e, poker.figure + 4); ++figure) {
            if (map.get(figure) > 0) {
                ++up;
            } else {
                break;
            }
        }

        if (up + down + 1 >= 5) {
            return 1;
        }

        return 0;
    }

    public void skillDelay(Long time) {
        skillDelaytime = time;
        skillDelayDoing = 1;
    }

    public boolean resume(Player player) {
        String uuid = player.getUuid();
        for (int i = 0; i < 3; ++i) {
            if (players.get(i).getUuid().equals(uuid)) {
                players.set(i, player);
                pause = 1;
                return true;
            }
        }

        return false;
    }

    public GameResumeMsg getResumeMsg(String uuid) {
        int index = -1;
        int[] initialHP = new int[3];
        for (int i = 0; i < 3; ++i) {
            initialHP[i] = Game.roleHP[playersRole[i]];
            if (players.get(i).getUuid().equals(uuid)) {
                index = i;
            }
        }
        if (index == -1) {
            return null;
        }

        return new GameResumeMsg(pokerHands[index], candidateCards[index], players, playersRole, initialHP, playersHP, lordSeal, luckyCard, luckyCardOwn);
    }

    public GameOverMsg getOverMsg() {
        return new GameOverMsg(totalDamage, playedLucky, playedSkill, playedBoom, end);
    }

    public int broadMsg(String msg) {
        int num = 0;
        for (int i = 0; i < 3; ++i) {
            if (!players.get(i).dead) {
                players.get(i).sendMsg(msg);
            } else {
                ++num;
            }
        }

        return num;
    }

    synchronized public boolean changeState(GameState src, GameState dst) {
        if (state == src) {
            state = dst;
            return true;
        }

        return false;
    }
}
