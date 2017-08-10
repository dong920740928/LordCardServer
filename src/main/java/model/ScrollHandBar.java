package model;

import msg.ScrollPickMsg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import poker.Hand;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yizhe on 17-8-4.
 */
public class ScrollHandBar {
    private static final Logger LOGGER = LogManager.getLogger(ScrollHandBar.class);

    public LinkedList<ScrollHand> scrollHands;

    public ScrollHandBar() {
        this.scrollHands = new LinkedList<ScrollHand>();
    }

    public void addHand(Hand hand, Long time) {
        scrollHands.offer(new ScrollHand(hand, time));
    }

    public ScrollPickMsg pickHands(Hand opHand) {
        ArrayList<ArrayList<Integer>> cards = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> positions = new ArrayList<Integer>();
        int damage = 0;
        for (int i = 0; i < scrollHands.size(); ++i) {
            int res = Hand.compare(opHand,scrollHands.get(i).hand);
            LOGGER.info("compare hand src: " + opHand.cards + " dst: " + scrollHands.get(i).hand.cards + " " + res);
            if (res == 1) {
                cards.add(scrollHands.get(i).hand.cards);
                damage += scrollHands.get(i).hand.damage;
                positions.add(i);
                scrollHands.remove(i);
                --i;
            }
        }

        if (positions.size() == 0) {
            return null;
        }

        return new ScrollPickMsg(cards, positions, damage);
    }

    public ArrayList<Integer> delHand(Long time) {
        if (scrollHands.size() > 0 && time > scrollHands.peek().time + Game.scrollDuration) {
            return scrollHands.poll().hand.cards;
        }

        return null;
    }

    private class ScrollHand {
        public Hand hand;
        public Long time;

        public ScrollHand(Hand hand, Long time) {
            this.hand = hand;
            this.time = time;
        }
    }
}
