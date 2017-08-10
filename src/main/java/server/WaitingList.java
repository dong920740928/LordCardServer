package server;

import model.Player;

import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by dong on 9/07/2017.
 */
public class WaitingList {
    private static final Logger LOGGER = LogManager.getLogger(WaitingList.class);

    private class MatchTicket {
        public Player player;
        Long time;

        public MatchTicket(Player player, Long time) {
            this.player = player;
            this.time = time;
        }
    }

    private LinkedBlockingQueue<MatchTicket> tickets;
    private Hashtable<String, Long> playersTicket;

    public WaitingList() {
        tickets = new LinkedBlockingQueue<MatchTicket>();
        playersTicket = new Hashtable<String, Long>();
    }

    public void enQueue(Player player) {
        Long time = System.currentTimeMillis();
        MatchTicket matchTicket = new MatchTicket(player, time);
        playersTicket.put(player.getUuid(), time);
        tickets.offer(matchTicket);
    }

    public Player deQueue() throws InterruptedException{
        Player player = null;

        int ret = 0;
        while (ret == 0) {
            MatchTicket matchTicket = null;
            matchTicket = tickets.take();
            player = matchTicket.player;
            if (ticketValid(matchTicket)) {
                ret = 1;
            }
        }

        return player;
    }

    public int cancel(String uuid) {
        playersTicket.remove(uuid);

        return 0;
    }

    private boolean ticketValid(MatchTicket matchTicket) {
        if (playersTicket.get(matchTicket.player.getUuid()) == null) {
            return false;
        }

        return matchTicket.time == playersTicket.get(matchTicket.player.getUuid());
    }
}
