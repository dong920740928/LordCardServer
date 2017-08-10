package server;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import model.PokerPool;
import msg.*;
import org.json.JSONArray;
import org.json.JSONObject;
import model.Game;
import model.Player;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import poker.Hand;

/**
 * Created by dong on 8/07/2017.
 */
public class MainServer {
    public static final int GAME_PORT = 20001;
    public static final int CONTROL_PORT = 14444;

    public static final int KEEP_ALIVE_TIME_OUT = 2 * 1000; //心跳包定时
    public static final int MAX_NO_RESPONSE = 5; //5个周期无应答认为断线
    public static final int MAX_PACKET_PER_SEC = 50; //正常情况下消息流量为每秒50个消息
    public static final int MAX_SUS_SEC = 5; //5秒钟流量异常, 认为该连接为恶意的

    private static final Logger LOGGER = LogManager.getLogger(MainServer.class);

    private ServerSocket server;

    private LoginManager loginManager;
    private WaitingList waitingList;
    private GameManager gameManager;

    private ControlThread controlThread;
    private ListenThread listenThread;
    private ArrayList<HandlerThread> handlerThreads;
    private ArrayList<GameThread> gameThreads;

    public static void main(String[] args) throws Exception {

        LOGGER.info("start...");
        MainServer mainServer = new MainServer();
        //游戏参数初始化
        Game.setGameArgs();

//        for (int i = 0; i < 100; ++i) {
//            int [] arr = Game.randPerm(3);
//            LOGGER.info(arr[0] + " " + arr[1] + " " + arr[2]);
//            Thread.sleep(500);
//        }

        LOGGER.info("init...");
        mainServer.init();

        LOGGER.info("working...");
        mainServer.work();

        mainServer.exit();
        LOGGER.info("exit...");
        System.exit(0);
    }

    private void init() {
        loginManager = new LoginManager();
        waitingList = new WaitingList();
        gameManager = new GameManager();

        this.controlThread = new ControlThread();
        this.listenThread = new ListenThread();
        this.handlerThreads = new ArrayList<HandlerThread>();
        this.gameThreads = new ArrayList<GameThread>();

        controlThread.start();
        listenThread.start();
    }

    private void work() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(CONTROL_PORT);
            byte[] recv = new byte[1024];
            while (true) {
                DatagramPacket recvPacket = new DatagramPacket(recv, recv.length);
                serverSocket.receive(recvPacket);
                String cmd = new String(recvPacket.getData(), recvPacket.getOffset(), recvPacket.getLength());
                byte[] b = cmd.getBytes();
                for (int i = 0; i < b.length; ++i) {
                    LOGGER.info(i + " " + b[i]);
                }
                if (cmd.equals("exit" + "\n")) {
                    break;
                } else if (cmd.equals("statics" + "\n")) {
                    LOGGER.info("worker: players num: " + loginManager.getPlayersNum() + " games mapping num: " + gameManager.getGamesNum());
                }
            }
        } catch (IOException e) {
            LOGGER.error("work error: " + e.getMessage());
        }
    }

    private void exit() {
        this.listenThread.exit();
        this.controlThread.exit();

        for (int i = 0; i < handlerThreads.size(); ++i) {
            handlerThreads.get(i).exit();
        }

        for (int i = 0; i < gameThreads.size(); ++i) {
            gameThreads.get(i).exit();
        }
        loginManager.destroy();
    }

    private class ListenThread extends MyThread {

        public ListenThread() {
            super();
        }

        public void run() {
            while (running) {
                try {
                    server = new ServerSocket(GAME_PORT);

                    LOGGER.info("server start ip: " + server.getInetAddress().getHostAddress() + " port: " + server.getLocalPort());

                    while (true) {
                        Socket client = server.accept();

                        //心跳包计时
                        client.setSoTimeout(KEEP_ALIVE_TIME_OUT);
                        HandlerThread handlerThread = new HandlerThread(client);
                        handlerThreads.add(handlerThread);
                        handlerThread.start();
                    }
                } catch (IOException e) {
                    LOGGER.error("server init error: " + e.getMessage());
                }
            }
        }
    }

    private class HandlerThread extends MyThread {
        private Socket socket;

        public HandlerThread(Socket socket) {
            super();
            this.socket = socket;
        }

        public void run() {
            String uuid = null;
            PrintStream output = null;
            BufferedReader input = null;

            try {
                output = new PrintStream(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                LOGGER.error("login error1:" + e.getMessage());
                return;
            }

            //login loop
            //{"action":"login","uuid":"001"}
            boolean waitLogin = true;
            int noResponseCycle; //无应答周期
            Player player;
            int susSec = 0;
            int msgCount = 0;
            Long time = System.currentTimeMillis();

            while (running) {
                noResponseCycle = 0;
                player = null;
                while (waitLogin) {
                    String loginMsg = null;
                    try {
                        loginMsg = input.readLine();
                        if (loginMsg == null) {
                            throw new IOException();
                        }
                    } catch (SocketTimeoutException e1) { //超时
                        LOGGER.info("login time out " + e1.getMessage());
                        ++noResponseCycle;
                        if (noResponseCycle == MAX_NO_RESPONSE) {
                            LOGGER.info("login disconnected.");
                            try {
                                socket.close();
                            } catch (IOException e11) {
                                LOGGER.error(e11.getMessage());
                            }
                            return;
                        } else {
                            continue;
                        }
                    } catch (IOException e) {
                        LOGGER.error("login reset: " + e.getMessage());
                        return;
                    }

                    Long recvTime = System.currentTimeMillis();
                    if (recvTime < time + 1000) {
                        ++msgCount;
                    } else {
                        if (msgCount > MAX_PACKET_PER_SEC) {
                            ++susSec;
                            if (susSec > MAX_SUS_SEC) {
                                LOGGER.info("ddos from " + socket.getInetAddress().getHostAddress());
                                try {
                                    socket.close();
                                } catch (IOException e11) {
                                    LOGGER.error(e11.getMessage());
                                }
                                return;
                            }
                        }
                        msgCount = 0;
                        time = recvTime;
                    }

                    JSONObject jsonLogin = new JSONObject(loginMsg);
                    String action = jsonLogin.getString("action");

                    if (action.equals("login")) {
                        //登陆请求
                        uuid = jsonLogin.getString("uuid");
                        if (loginManager.hasPlayer(uuid)) {
                            //用户已存在
                            output.println(new LoginResMsg(uuid, -1).toString());
                            LOGGER.info("repeat user: " + uuid);
                        } else if (gameManager.isPlaying(uuid) == 1) {
                            Game game = gameManager.getGame(uuid);
                            boolean res = game.changeState(Game.GameState.PLAYING, Game.GameState.PAUSED);
                            if (!res) {
                                //output.println(new LoginResMsg(uuid, 0));
                                output.println(game.getResumeMsg(uuid).toString());
                                output.println(game.getOverMsg().toString());
                                continue;
                            }
                            //用户被托管
                            waitLogin = false;
                            try {
                                player = new Player("tony", uuid, socket);
                            } catch (IOException e) {
                                LOGGER.error("user " + uuid + " reload disconnected. " + e.getMessage());
                                return;
                            }
                            game.resume(player);

                            output.println(new LoginResMsg(uuid, 0).toString());
                            LOGGER.info("output to " + uuid + ": " + "login reload");

                            while (game.pause == 1);
                            player.changeState(Player.State.ONLINE, Player.State.PLAYING);
                            output.println(game.getResumeMsg(uuid).toString());

                            loginManager.addPlayer(uuid, player);
                            LOGGER.info("user reload: " + uuid);
                            game.changeState(Game.GameState.PAUSED, Game.GameState.PLAYING);
                        } else {
                            //用户成功登陆
                            waitLogin = false;
                            try {
                                player = new Player("tom", uuid, socket);
                            } catch (IOException e) {
                                LOGGER.error("user " + uuid + " login disconnected. " + e.getMessage());
                                return;
                            }
                            loginManager.addPlayer(uuid, player);
                            LOGGER.info("player " + player.getUuid() + " login.");
                            output.println(new LoginResMsg(uuid, 1));
                            LOGGER.info("output to " + uuid +": " + "login");
                        }
                    } else if (action.equals("keep_alive")) {
                        //心跳包
                        noResponseCycle = 0;
                        output.println(new KeepAliveMsg().toString());
                    } else {
                        //非法请求
                        output.println(new LoginResMsg("admin", -1).toString());
                        LOGGER.info("invalid login msg: " + loginMsg);
                    }
                }

                //接收原始消息
                while (!waitLogin) {
                    String msg = null;
                    try {
                        msg = player.recv();
                        if (msg == null) {
                            throw new IOException();
                        }
                    } catch (SocketTimeoutException e1) {
                        LOGGER.info("player " + player.getUuid() + " recv time out: " + e1.getMessage());
                        ++player.noResponse;
                        if (player.noResponse == MAX_NO_RESPONSE) {
                            player.dead = true;
                            LOGGER.info("player " + player.getUuid() + " disconnected.");
                            loginManager.delPlayer(player.getUuid());
                            player.release();
                            return;
                        } else {
                            continue;
                        }
                    } catch (IOException e2) {
                        LOGGER.error("player " + player.getUuid() + " recv error: " + e2.getMessage());
                        player.dead = true;
                        loginManager.delPlayer(player.getUuid());
                        player.release();
                        return;
                    }

                    Long recvTime = System.currentTimeMillis();
                    if (recvTime < time + 1000) {
                        ++msgCount;
                    } else {
                        if (msgCount > MAX_PACKET_PER_SEC) {
                            ++susSec;
                            if (susSec > MAX_SUS_SEC) {
                                LOGGER.info("ddos from " + socket.getInetAddress().getHostAddress());
                                try {
                                    socket.close();
                                } catch (IOException e11) {
                                    LOGGER.error(e11.getMessage());
                                }
                                return;
                            }
                        }
                        msgCount = 0;
                        time = recvTime;
                    }

                    JSONObject object = new JSONObject(msg);
                    String action = object.getString("action");

                    if (action.equals("keep_alive")) {
                        //收到心跳包
                        player.noResponse = 0;
                        player.send(new KeepAliveMsg().toString());
                    } else if (action.equals("exit")) {
                        //玩家退出登录
                        LOGGER.info("player " + player.getUuid() + " exit.");
                        loginManager.delPlayer(player.getUuid());
                        waitLogin = true;
                        player.dead = true;
                        player = null;
                        break;
                    } else if (action.equals("matching_req")) {
                        //匹配请求
                        boolean res = player.changeState(Player.State.ONLINE, Player.State.WAITING);
                        if (res) {
                            waitingList.enQueue(player);
                            LOGGER.info("player " + player.getUuid() + " waiting...");
                        } else {
                            LOGGER.info("player " + player.getUuid() + " bad matching req: state" + player.state);
                        }
                    } else if (action.equals("matching_cancel")) {
                        //取消匹配请求
                        boolean res = player.changeState(Player.State.WAITING, Player.State.ONLINE);
                        if (res) {
                            player.send(new MatchingCancelResMsg().toString());
                            LOGGER.info("player " + player.getUuid() + " cancel matching");
                            waitingList.cancel(player.getUuid());
                            LOGGER.info("player " + player.getUuid() + " exit.");
                            loginManager.delPlayer(player.getUuid());
                            waitLogin = true;
                            player = null;
                            break;
                        } else {
                            LOGGER.info("player " + player.getUuid() + " bad matching cancel req: state " + player.state);
                        }
                    } else if (action.equals("login")) {
                        //客户端状态错误 同步登陆结果
                        player.send(new LoginResMsg(player.getUuid(), 1).toString());
                    } else {
                        //收到玩家操作,拉取到接收队列
                        player.pullMsg(msg);
                        LOGGER.info("player " + player.getUuid() + " pull msg: " + msg);
                    }

                    //检查是否有消息推送给玩家
                    msg = player.pushMsg();
                    //LOGGER.info("push msg: " + msg);
                    if (msg != null) {
                        LOGGER.info("player " + player.getUuid() + " push msg: " + msg);
                        player.send(msg);
                    }
                }
            }
        }
    }

    private class ControlThread extends MyThread {
        public ControlThread() {}

        public void run() {
            while (running) {
                List<Player> players = new ArrayList<Player>();

                //从队列中取出三个玩家
                try {
                    for (int i = 0; i < 3; ++i) {
                        players.add(waitingList.deQueue());
                    }
                } catch (InterruptedException e) {
                    return;
                }

                //检查玩家连接
                int go = 1;
                for (int i = 0; i < 3; ++i) {
                    if (players.get(i).dead == true) {
                        go = -1;
                        break;
                    }
                }

                //锁定玩家, 不接受取消匹配
                for (int i = 0; i < 3; ++i) {
                    if (!players.get(i).changeState(Player.State.WAITING, Player.State.PLAYING)) {
                        go = -1;
                    }
                }

                if (go == 1) {
                    //成功匹配
                    GameThread gameThread = new GameThread(players);
                    gameThreads.add(gameThread);
                    gameThread.start();
                } else {
                    //有玩家中途断开连接, 将连接正常的玩家送回匹配队列
                    for (int i = 0; i < 3; ++i) {
                        if (players.get(i).dead == true) {
                            loginManager.delPlayer(players.get(i).getUuid());
                        } else if (players.get(i).state == Player.State.PLAYING) {
                            players.get(i).changeState(Player.State.PLAYING, Player.State.WAITING);
                            waitingList.enQueue(players.get(i));
                        }
                    }
                }
            }
        }
    }

    private class GameThread extends MyThread {
        private Game game;

        public GameThread(List<Player> players) {
            super();
            for (int i = 0; i < 3; ++i) {
                players.get(i).clearRecvBuf();
                players.get(i).clearRecvBuf();
            }
            this.game = new Game(players);
        }

        public void run() {
            gameManager.addGame(game);
            List<Player> players = game.players;

            //玩家角色选择
            game.chooseRole();

            //game argument setup
            game.initGame();

            //游戏开始消息
            GameStartMsg gameStartMsg = new GameStartMsg(game.players, game.playersRole);
            SettingMsg settingMsg = new SettingMsg(Game.settingsStr);
            for (int i = 0; i < 3; ++i) {
                players.get(i).sendMsg(gameStartMsg.toString());
                players.get(i).sendMsg(settingMsg.toString());
            }

            //重置lucky card
            game.luckyCard = -1;
            game.luckyCardOwn = -1;
            game.luckyTime = 0L;

            //Game.delay(Game.chooseRolesDelay);
            //waiting for players to choose role
            boolean[] ready = new boolean[] {false, false, false};
            int readyNum = 0;
            while (running) {
                Long time = System.currentTimeMillis();
                if (time > game.startTime + Game.chooseRolesDelay) {
                    break;
                }
                for (int i = 0; i < 3; ++i) {
                    //receive choose role msg of player i
                    String msg = players.get(i).getMsg();
                    if (msg != null) {
                        JSONObject object = new JSONObject(msg);
                        String action = object.getString("action");
                        if (action.equals("choose_role")) {
                            //player i choose his role
                            ChooseRoleMsg chooseRoleMsg = new ChooseRoleMsg(msg);
                            int index = chooseRoleMsg.index;
                            if (!ready[index]) {
                                ready[index] = true;
                                ++readyNum;
                                if (readyNum == 3) {
                                    break;
                                }
                            }
                        }
                    }
                }
                //all players ready
                if (readyNum == 3) {
                    break;
                }
            }

            //发牌
            for (int i = 0; i < 3; ++i) {
                players.get(i).sendMsg(new StartPokersMsg(game.pokerHands[i], game.playersRole, game.playersHP).toString());
            }

            int check = check(players);
            if (check == 3) {
                return;
            }

            //等待客户端过场动画
            Game.delay(Game.startGameDelay);

            //牌局开始
            game.end = 0; //游戏进行状态
            game.changeState(Game.GameState.READY, Game.GameState.PLAYING);

            while (true) {
                int downNum = check(players);
                if (downNum == 3) {
                    break;
                }

                for (int i = 0; i < 3; ++i) {
                    //检查玩家i的消息队列
                    Player player = players.get(i);
                    if (game.pause == 1) {
                        game.pause = 0;
                        Game.delay(Game.reconnectDelay);
                    }
                    if (player.dead) {
                        //托管
                        continue;
                    }
                    //处理玩家游戏逻辑
                    Long time = System.currentTimeMillis();
                    String msg = null;


                    //update game delay
                    if (game.skillDelayDoing == 1) {
                        if (time > game.skillDelaytime + Game.skillDelay) {
                            game.skillDelayDoing = -1;
                            game.skillDelaytime = 0L;
                        }
                    }

                    msg = player.getMsg();
                    Long time1 = System.currentTimeMillis();

                    if (msg != null) {
                        LOGGER.info("get cost: "+ (time1 - time) + player.getUuid());

                        //接收消息
                        LOGGER.info("get msg from user " + player.getUuid() + " : " + msg);
                        JSONObject jsonPlay = new JSONObject(msg);
                        String action = jsonPlay.getString("action");

                        if ("poker_play".equals(action)) {
                            //有人出牌
                            PokerPlayMsg pokerPlayMsg = new PokerPlayMsg(msg);
                            int index = pokerPlayMsg.index;
                            ArrayList<Integer> cards = pokerPlayMsg.pokersPlayed;
                            int clientDamage = pokerPlayMsg.clientDamage;
                            int damage = 0;

                            LOGGER.info("get hand from " + player.getUuid());
                            Hand hand = new Hand(cards);
                            Game.HandResCode handResCode = game.playSuccess(index, hand, time);

                            if (handResCode != Game.HandResCode.SUCCESS) {
                                //手牌非法
                                player.sendMsg(new HandValidMsg(false, cards, 0, game.playersPower[index], handResCode.ordinal()).toString());
                                Long time2 = System.currentTimeMillis();
                                LOGGER.info("response time: " + (time2 - time1) + "ms");
                            } else {
                                //打出了牌
                                game.playCardTime[index] = time;
                                damage = hand.damage;

                                //能量
                                int powerAdd = game.getPower(index, hand);
                                if (game.lordSeal != -1 && index != game.lord) {
                                    //monkey and tangMonk can not add power during seal of lord.
                                    powerAdd = 0;
                                }
                                game.playersPower[index] += Math.min(powerAdd, game.playersMaxPower[index] - game.playersPower[index]);

                                player.sendMsg(new HandValidMsg(true, cards, powerAdd, game.playersPower[index], handResCode.ordinal()).toString());
                                Long time2 = System.currentTimeMillis();
                                LOGGER.info("response time: " + (time2 - time1) + "ms");

                                if (powerAdd > 0) {
                                    game.broadMsg(new PowerStatMsg(game.playersPower, game.playersMaxPower).toString());
                                }

                                if (game.playersPower[index] == game.playersMaxPower[index]) {
                                    //能量满槽
                                    if (index == game.lord && game.lordSeal == -1) {
                                        //牛魔王封印
                                        game.lordSeal = 5;
                                        game.playedSkill[game.lord]++;
                                        game.skillDelay(time);
                                    } else if (index == game.tangMonk && game.tangMonkFrozen == -1) {
                                        //tangMonk freeze spade card of lord
                                        game.tangMonkFrozen = 1;
                                        game.frozenTime = time;
                                        game.playedSkill[game.tangMonk]++;
                                        game.skillDelay(time);
                                    } else if (index == game.monkey && game.monkeyShield == -1) {
                                        game.monkeyShield = 1;
                                        game.playedSkill[game.monkey]++;
                                        game.skillDelay(time);
                                    }
                                }

                                LOGGER.info("static 1: " + player.getUuid());
                                //炸弹统计
                                if (hand.type == Hand.Type.BOOM) {
                                    game.playedBoom[index]++;
                                }

                                //悟空暴击
                                if (index == game.monkey && game.monkeyShield == 1) {
                                    damage += Game.monkeyStickDamageBase + Game.monkeyStickDamageFactor * game.playersPower[game.lord];
                                }

                                //是否打出lucky card
                                boolean lucky = Game.hasLucky(cards);
                                if (lucky) {
                                    //伤害翻倍
                                    damage *= Game.luckyDamageFactor;
                                }

                                LOGGER.info("player " + player.getUuid() + " played hand: type " + hand.type + " damage " + hand.damage + " figure " + hand.figure + " length  " + hand.length);
                                LOGGER.info("client damage: " + clientDamage + " compare: " + (hand.damage == clientDamage));

                                //攻击结算
                                if (index != game.lord) {
                                    //师徒攻击?
                                    if (game.lordSeal != -1) {
                                        //牛魔王封印存在
                                        int spadeNum = hand.patternNum(Hand.Pattern.SPADE.ordinal());
                                        game.lordSeal = game.lordSeal < spadeNum ? 0 : game.lordSeal - spadeNum;
                                        damage = 0;
                                    } else {
                                        game.mentorRecentHand = new Hand(hand);
                                        game.mentorRecentIndex = index;

                                        //scroll pick msg
                                        ScrollPickMsg scrollPickMsg = game.mentorBar.pickHands(hand);
                                        if (scrollPickMsg != null) {
                                            String msgStr = scrollPickMsg.toString();
                                            damage += scrollPickMsg.damage;
                                            players.get(game.monkey).sendMsg(msgStr);
                                            players.get(game.tangMonk).sendMsg(msgStr);
                                        }

                                        int hp = game.playersHP[game.lord];

                                        game.totalDamage[index] += Math.min(hp, damage);

                                        if (hp <= damage) {
                                            game.playersHP[game.lord] = 0;
                                            //师徒获胜
                                            game.end = 1;
                                        } else {
                                            game.playersHP[game.lord] = hp - damage;
                                        }
                                    }
                                } else {
                                    //牛魔王攻击
                                    game.lordRecentHand = new Hand(hand);

                                    //scroll pick msg
                                    ScrollPickMsg scrollPickMsg = game.lordBar.pickHands(hand);
                                    if (scrollPickMsg != null) {
                                        String msgStr = scrollPickMsg.toString();
                                        damage += scrollPickMsg.damage;
                                        players.get(game.lord).sendMsg(msgStr);
                                    }

                                    int monkeyHP = game.playersHP[game.monkey];
                                    int tangMonkHP = game.playersHP[game.tangMonk];

                                    game.playersHP[game.monkey] = monkeyHP < damage ? 0 : monkeyHP - damage;
                                    game.playersHP[game.tangMonk] = tangMonkHP < damage ? 0 : tangMonkHP - damage;

                                    game.totalDamage[index] += Math.min(monkeyHP, damage);
                                    game.totalDamage[index] += Math.min(tangMonkHP, damage);

                                    if (game.playersHP[game.monkey] == 0 && game.playersHP[game.tangMonk] == 0) {
                                        //牛魔王获胜
                                        game.end = 2;
                                    }
                                }

                                LOGGER.info("static 2: " + player.getUuid());
                                //游戏状态广播
                                PokerStatMsg pokerStatMsg = new PokerStatMsg(index, game.end, game.playersHP, cards, damage, lucky ? game.luckyCard : -1, game.lordSeal, game.tangMonkFrozen, game.monkeyShield);
                                for (int j = 0; j < 3; ++j) {
                                    players.get(j).sendMsg(pokerStatMsg.toString());
                                }

                                //扣除手牌
                                game.pokerHands[index].removeAll(cards);

                                //如果打出了lucky card
                                if (lucky) {
                                    //lucky card 统计
                                    game.playedLucky[game.luckyCardOwn]++;

                                    //lucky card 不会放回牌库
                                    cards.remove((Integer) game.luckyCard);

                                    //重置玩家手牌上限
                                    game.playerCardsNum[game.luckyCardOwn]--;

                                    //重置lucky card
                                    game.luckyCard = -1;
                                    game.luckyCardOwn = -1;
                                    game.luckyTime = 0L;
                                }
                                LOGGER.info("static 3: " + player.getUuid());

                                //将出过的牌放回牌库
                                game.pokerPools[index].putPokers(cards);

                                //从候选队列向手牌补牌
                                List<Integer> supply = game.supplyCards(index);
                                game.pokerHands[index].addAll(supply);
                                player.sendMsg(new SupplyPokerMsg(supply).toString());

                                LOGGER.info("static 4: " + player.getUuid());

                                //lucky card active or change owner
                                if (game.end == 0) {
                                    if (game.luckyCard == -1 && index != game.lord) {
                                        if (game.lordSeal == -1 && hand.type == Hand.Type.SHUNZI) {
                                            if (Game.isLucky()) {
                                                //师徒二人激活luckycard
                                                game.luckyCard = Game.chooselucky(cards);
                                                game.luckyCardOwn = 3 - game.lord - index;
                                                game.luckyTime = time;
                                                game.pokerHands[game.luckyCardOwn].add(game.luckyCard);
                                                game.playerCardsNum[game.luckyCardOwn]++;

                                                //广播lucky card msg
                                                for (int j = 0; j < 3; ++j) {
                                                    players.get(j).sendMsg(new LuckyCardMsg(index, game.luckyCardOwn, game.luckyCard, Game.luckyCardLive).toString());
                                                }
                                            }
                                        }
                                    } else if (game.luckyCard != -1 && index == game.lord) {
                                        //牛魔王抢走luckycard
                                        if (hand.type == Hand.Type.SHUNZI && game.luckyCardOwn != game.lord) {
                                            //旧主人失去lucky card
                                            --game.playerCardsNum[game.luckyCardOwn];
                                            game.pokerHands[game.luckyCardOwn].remove((Integer)(game.luckyCard));

                                            //广播lucky card msg
                                            for (int j = 0; j < 3; ++j) {
                                                players.get(j).sendMsg(new LuckyCardMsg(game.luckyCardOwn, index, game.luckyCard, Game.luckyCardLive).toString());
                                            }

                                            //新主人获得lucky card
                                            game.luckyTime = time;
                                            game.luckyCardOwn = index;
                                            game.pokerHands[game.luckyCardOwn].add(game.luckyCard);
                                            ++game.playerCardsNum[game.luckyCardOwn];
                                        }
                                    }
                                }

                                LOGGER.info("static 5: " + player.getUuid());
                                //牛魔王封印解除, 重置牛魔王能量
                                if (game.lordSeal == 0) {
                                    game.lordSeal = -1;
                                    game.playersPower[game.lord] = 0;
                                    game.broadMsg(new PowerStatMsg(game.playersPower, game.playersMaxPower).toString());
                                }

                                LOGGER.info("static 6: " + player.getUuid());

                                //monkey stick reset
                                if (game.monkeyShield == 1) {
                                    game.monkeyShield = -1;
                                    game.playersPower[game.monkey] = 0;
                                    game.broadMsg(new PowerStatMsg(game.playersPower, game.playersMaxPower).toString());
                                }

                                LOGGER.info("static 7: " + player.getUuid());

                                //游戏结束
                                if (game.end != 0) {
                                    break;
                                }
                            }
                        }
                    }

                    if (game.end != 0) {
                        break;
                    }

                    //tangMonk frozen timer
                    if (game.tangMonkFrozen == 1 && time > game.frozenTime + Game.tangMonkFrozenDuration) {
                        //frozen expired
                        for (int j = 0; j < 3; ++j) {
                            players.get(j).sendMsg(new EventStatMsg(false, Game.EventID.FROZEN.ordinal()).toString());
                        }
                        game.tangMonkFrozen = -1;
                        game.frozenTime = 0L;
                        game.playersPower[game.tangMonk] = 0;
                        game.broadMsg(new PowerStatMsg(game.playersPower, game.playersMaxPower).toString());
                    }

                    //lord scroll bar addition
                    if (time > game.lordBarAddTime + Game.scrollAddCD) {
                        game.lordBarAddTime = time;
                        if (game.mentorRecentHand != null) {
                            game.lordBar.addHand(game.mentorRecentHand, time);
                            players.get(game.lord).sendMsg(new ScrollAddMsg(game.mentorRecentHand.cards, game.lordBar.scrollHands.size(), game.mentorRecentIndex).toString());
                            game.mentorRecentHand = null;
                            game.mentorRecentIndex = -1;
                        }
                    }
                    ArrayList<Integer> expired = null;

                    //lord scroll bar expire
                    if ((expired = game.lordBar.delHand(time)) != null) {
                        players.get(game.lord).sendMsg(new ScrollDelMsg(expired, 0).toString());
                    }

                    //mentor scroll bar addition
                    if (time > game.mentorBarAddTime + Game.scrollAddCD) {
                        game.mentorBarAddTime = time;
                        if (game.lordRecentHand != null) {
                            game.mentorBar.addHand(game.lordRecentHand, time);
                            String addMsg1 = new ScrollAddMsg(game.lordRecentHand.cards, game.mentorBar.scrollHands.size(), game.lord).toString();
                            players.get(game.monkey).sendMsg(addMsg1);
                            String addMsg2 = new ScrollAddMsg(game.lordRecentHand.cards, game.mentorBar.scrollHands.size(), game.lord).toString();
                            players.get(game.tangMonk).sendMsg(addMsg2);
                            game.lordRecentHand = null;
                        }
                    }

                    //mentor scroll bar expired
                    if ((expired = game.mentorBar.delHand(time)) != null) {
                        String delMsg1 = new ScrollDelMsg(expired, 0).toString();
                        players.get(game.monkey).sendMsg(delMsg1);
                        String delMsg2 = new ScrollDelMsg(expired, 0).toString();
                        players.get(game.tangMonk).sendMsg(delMsg2);
                    }

                    //lucky card timer
                    if (game.luckyCard != -1) {
                        if (time > game.luckyTime + Game.luckyCardLive) {
                            //lucky card 超时
                            //从owner手牌移除lucky card
                            for (int j = 0; j < 3; ++j) {
                                players.get(j).sendMsg(new EventStatMsg(false, Game.EventID.LUCKY.ordinal()).toString());
                            }

                            game.playerCardsNum[game.luckyCardOwn]--;
                            game.pokerHands[game.luckyCardOwn].remove((Integer)(game.luckyCard));
                            game.luckyCard = -1;
                            game.luckyCardOwn = -1;
                            game.luckyTime = 0L;
                        }
                    }

                    //检查玩家i的状态(补牌/CD)
                    //抽牌
                    if (game.skillDelayDoing == -1) {
                        if (time > game.drawCardTime[i] + Game.drawCardCD) {
                            game.drawCardTime[i] = time;
                            if (game.candidateCards[i].size() < Game.candidateNum) {
                                //int poker = game.pokerPools[i].getPoker();
                                int poker = game.drawPoker(i);
                                List<Integer> pokers = new ArrayList<Integer>();
                                pokers.add(poker);
                                player.sendMsg(new DrawPokerMsg(pokers).toString());
                                game.candidateCards[i].offer(poker);

                                //if player has no hand card, supply card from candidate queue.
                                if (game.pokerHands[i].size() == 0) {
                                    List<Integer> supply = game.supplyCards(i);
                                    game.pokerHands[i].addAll(supply);
                                    player.sendMsg(new SupplyPokerMsg(supply).toString());
                                }
                            }
                        }
                    }
                }

                if (game.end != 0) {
                    break;
                }
            }
            while (!game.changeState(Game.GameState.PLAYING, Game.GameState.END)) {
                game.pause = 0;
            }

            for (int i = 0; i < 3; ++i) {
                players.get(i).sendMsg(game.getOverMsg().toString());
            }

            gameManager.delGame(game);
            //reset(players);
        }
    }

    private class MyThread extends Thread {
        protected boolean running;

        public MyThread() {
            super();
            this.running = true;
        }

        public void exit() {
            this.running = false;
            this.interrupt();
        }
    }

    //存活玩家重新加入匹配队列
    public int reset(List<Player> players) {
        for (int i = 0; i < 3; ++i) {
            Player player = players.get(i);
            if (player.dead) {
                loginManager.delPlayer(player.getUuid());
            } else {
                player.sendMsg(new ReconnectMsg().toString());
                waitingList.enQueue(player);
            }
        }

        return 0;
    }

    //检查断线人数
    public int check(List<Player> players) {
        int res = 0;
        for (int i = 0; i < 3; ++i) {
            res += players.get(i).dead ? 1 : 0;
        }

        return res;
    }
}
