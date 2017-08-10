package poker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.GameManager;

import java.util.*;

/**
 * Created by dong on 13/07/2017.
 */
public class Hand {
    private static final Logger LOGGER = LogManager.getLogger(Hand.class);

    public enum Type {
        INVALID,
        SINGLE,
        DOUBLE,
        TRIPLE,
        TRIPLE_PLUS_SINGLE,
        TRIPLE_PLUS_DOUBLE,
        FOUR_PLUS_TWO,
        SHUNZI,
        SHUNZI_DOUBLE,
        PLANE,
        PLANE_SINGLE_PLUS,
        PLANE_DOUBLE_PLUS,
        BOOM,
        JOKER_BOOM,
    }

    public enum Pattern{
        DIAMOND,
        CLUBS,
        HEART,
        SPADE,
    }

    public int figure;
    public int length;
    public Type type;
    public int damage;
    public ArrayList<Integer> cards;

    public static Map<Integer, Integer> getMap(List<Integer> list) {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (int i = 0x03; i <= 0x11; ++i) {
            map.put(i, 0);
        }

        //map 存储出现的点数及对应个数
        for (int i = 0; i < list.size(); ++i) {
            int num = map.remove(new Poker(list.get(i)).figure);
            map.put(new Poker(list.get(i)).figure, num + 1);
        }

        return map;
    }

    public static int compare(Hand src, Hand dst) {
        if (src.type == Type.INVALID || dst.type == Type.INVALID) {
            return -2;
        }

        if (src.type == Type.JOKER_BOOM) {
            if (dst.type != Type.JOKER_BOOM) {
                return 1;
            } else {
                return 0;
            }
        }

        if (dst.type == Type.JOKER_BOOM) {
            return -1;
        }

        if (src.type == Type.BOOM) {
            if (dst.type == Type.BOOM) {
                if (src.figure > dst.figure) {
                    return 1;
                } else if (src.figure == dst.figure) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                return 1;
            }
        }

        if (dst.type == Type.BOOM) {
            return -1;
        }

        if (src.type == dst.type && src.length == dst.length) {
            if (src.figure > dst.figure) {
                return 1;
            } else if (src.figure == dst.figure) {
                return 0;
            } else {
                return -1;
            }
        }

        return 0;
    }

    private int isSeries(List<Integer> list) {
        int series = 1;

        Collections.sort(list);
        for (int i = 0; i < list.size() - 1; ++i) {
            if (list.get(i) + 1 != list.get(i + 1)) {
                series = -1;
                break;
            }
        }

        return series;
    }

    public Hand(ArrayList<Integer> list) {
        type = Type.INVALID;
        cards = list;

        for (int i = 0; i < list.size(); ++i) {
            if (!new Poker(list.get(i)).valid) {
                //出现非法的牌id
                return;
            }
        }

        Map<Integer, Integer> map = getMap(list);

        //tables[i]存储出现i+1次的点数集合
        List<List<Integer>> tables = new ArrayList<List<Integer>>();
        for (int i = 0; i < 4; ++i) {
            tables.add(new ArrayList<Integer>());
        }

        Iterator<Map.Entry<Integer, Integer>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            Integer key = entry.getKey();
            Integer val = entry.getValue();
            if (val > 0 && val <= 4) {
                tables.get(val - 1).add(key);
            } else if (val > 4) {
                return;
            }
        }

        for (int i = 0; i < 4; ++i) {
            Collections.sort(tables.get(i));
        }

        int code = 0;
        for (int i = 0; i < 4; ++i) {
            code += (tables.get(i).size() << (i * 4));
        }

        LOGGER.info("cards: " + list + " code: " + code);
        switch (code) {
            case 0x1010:
                //四带一对 暂时禁用
                //type = Type.FOUR_PLUS_TWO;
                type = Type.INVALID;
                figure = tables.get(3).get(0);
                length = 1;
                //damage = 40;
                damage = 0;
                break;
            case 0x1002:
                //四带两张 暂时禁用
                //type = Type.FOUR_PLUS_TWO;
                type = Type.INVALID;
                figure = tables.get(3).get(0);
                length = 1;
                //damage = 40;
                damage = 0;
                break;
            case 0x1000:
                //炸弹
                type = Type.BOOM;
                figure = tables.get(3).get(0);
                length = 1;
                damage = 40;
                break;
            case 0x0110:
                //三带一对
                type = Type.TRIPLE_PLUS_DOUBLE;
                figure = tables.get(2).get(0);
                length = 1;
                damage = 30;
                break;
            case 0x0101:
                //三带一张
                type = Type.TRIPLE_PLUS_SINGLE;
                figure = tables.get(2).get(0);
                length = 1;
                damage = 25;
                break;
            case 0x0100:
                //三不带
                type = Type.TRIPLE;
                figure = tables.get(2).get(0);
                length = 1;
                damage = 20;
                break;
            case 0x0010:
                //一对
                type = Type.DOUBLE;
                figure = tables.get(1).get(0);
                length = 1;
                damage = 10;
                break;
            case 0x0001:
                figure = tables.get(0).get(0);
                length = 1;
                type = Type.SINGLE;
                if (figure == 0x10) {
                    //小王
                    damage = 10;
                } else if (figure == 0x11) {
                    //大王
                    damage = 10;
                } else {
                    //一张
                    damage = 4;
                }
                break;
            case 0x0002:
                if (tables.get(0).get(0) == 0x10 && tables.get(0).get(1) == 0x11) {
                    //王炸
                    type = Type.JOKER_BOOM;
                    figure = tables.get(0).get(0);
                    length = 1;
                    damage = 60;
                }
                break;
            default:
                if (tables.get(3).size() == 0) {
                    if (tables.get(2).size() > 1) {
                        //飞机
                        if (isSeries(tables.get(2)) == 1) {
                            if (tables.get(2).get(tables.get(2).size() - 1) != 0x0f) { //不能飞到2
                                if (tables.get(1).size() * 2 + tables.get(0).size() == tables.get(2).size()) {
                                    //飞机带小翼
                                    type = Type.PLANE_SINGLE_PLUS;
                                    figure = tables.get(2).get(0);
                                    length = tables.get(2).size();
                                    damage = 20 + 25 * length;
                                } else if (tables.get(0).size() == 0 && tables.get(1).size() == tables.get(2).size()) {
                                    //飞机带大翼
                                    type = Type.PLANE_DOUBLE_PLUS;
                                    figure = tables.get(2).get(0);
                                    length = tables.get(2).size();
                                    damage = 20 + 30 * length;
                                } else if (tables.get(1).size() == 0 && tables.get(0).size() == 0) {
                                    //飞机不带翼
                                    type = Type.PLANE;
                                    figure = tables.get(2).get(0);
                                    length = tables.get(2).size();
                                    damage = 20 + 20 * length;
                                }
                            }
                        }
                    } else if (tables.get(2).size() == 0) {
                        if (tables.get(1).size() >= 3 && isSeries(tables.get(1)) == 1) {
                            //连对
                            if (tables.get(1).get(tables.get(1).size() - 1) != 0x0f) { //不能连到2
                                if (tables.get(0).size() == 0) {
                                    type = Type.SHUNZI_DOUBLE;
                                    figure = tables.get(1).get(0);
                                    length = tables.get(1).size();
                                    damage = 10 * length + 20;
                                }
                            }
                        } else if (tables.get(1).size() == 0) {
                            //顺子
                            if (tables.get(0).size() >= 5 && isSeries(tables.get(0)) == 1) {
                                if (tables.get(0).get(tables.get(0).size() - 1) != 0xf) { //不能顺到2
                                    type = Type.SHUNZI;
                                    figure = tables.get(0).get(0);
                                    length = tables.get(0).size();
                                    damage = 6 * length + 0;
                                }
                            }
                        }
                    }
                }
        }
    }

    public Hand(Hand hand) {
        this.figure = hand.figure;
        this.length = hand.length;
        this.type = hand.type;
        this.damage = hand.damage;
        this.cards = new ArrayList<Integer>();
        this.cards.addAll(hand.cards);
    }

    public int patternNum(int pattern) {
        int num = 0;
        for (int i = 0; i < cards.size(); ++i) {
            if ((cards.get(i) & 0x00f) == pattern) {
                ++num;
            }
        }

        return num;
    }

    public int doubleNum() {
        int num = 0;
        if (type == Type.DOUBLE) {
            num = 1;
        } else if (type == Type.SHUNZI_DOUBLE) {
            num = length;
        }

        return num;
    }

    public int tripleNum() {
        int num = 0;
        if (type == Type.TRIPLE ||
                type == Type.TRIPLE_PLUS_SINGLE ||
                type == Type.TRIPLE_PLUS_DOUBLE) {
            num = 1;
        } else if (type == Type.PLANE ||
                type == Type.PLANE_SINGLE_PLUS ||
                type == Type.PLANE_DOUBLE_PLUS) {
            num = length;
        }

        return num;
    }
}
