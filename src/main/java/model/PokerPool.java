package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by yizhe on 17-7-19.
 */
public class PokerPool {
    private List<Integer> pokers;
    private Random random;
    private Game game;

    /*
    0x030 方块3
    0x031 草花3
    0x032 红桃3
    0x033 黑桃3
    0x040 方块4
    ...
    0x0a0 方块10
    ...
    0x0b0 方块J
    ...
    0x0e0 方块A
    ...
    0x0f0 方块2
    ...
    0x104 小王
    0x114 大王
     */
    public PokerPool(int index, int zuobi, Game game) {
        pokers = new ArrayList<Integer>();
        random = new Random(System.currentTimeMillis() + index * 1000 * 60);
        this.game = game;

        //作弊
        if (zuobi == 1) {
            for (int i = 0; i < 64; ++i) {
                pokers.add(0x104);
                pokers.add(0x114);
            }
            return;
        }

        //每个玩家的初始牌库为一副手牌
        for (int pattern = 0x0; pattern < 0x4; ++pattern) {
            for (int figure = 0x3; figure <= 0xf; ++figure) {
                pokers.add((figure << 4) + pattern);
            }
        }

        pokers.add(0x104);//小王
        pokers.add(0x114);//大王
    }

    //将牌放入牌库
    public void putPokers(List<Integer> pokersId) {
        for (int i = 0; i < pokersId.size(); ++i) {
            pokers.add(pokersId.get(i));
        }
    }

    public void putPoker(int poker) {
        pokers.add(poker);
    }

    //从牌库中随机抽牌
    public int getPoker() {
        if (pokers.size() == 0) {
            //疲劳
            return -1;
        }

        int r = random.nextInt(pokers.size());
        int poker = pokers.remove(r);

        return poker;
    }

    public int getValuePoker(int index) {
        List<Integer> valueList = new ArrayList<Integer>();
        for (int i = 0; i < pokers.size(); ++i) {
            if (game.pokerValue(index, pokers.get(i)) == 1) {
                valueList.add(pokers.get(i));
            }
        }

        if (valueList.size() == 0) {
            return -1;
        }
        int r = random.nextInt(valueList.size());
        pokers.remove(new Integer(valueList.get(r)));
        return valueList.get(r);
    }
}
