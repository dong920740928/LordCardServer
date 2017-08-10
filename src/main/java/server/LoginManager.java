package server;

import org.json.JSONObject;
import model.Player;

import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by dong on 10/07/2017.
 */
public class LoginManager {
    private static final Logger LOGGER = LogManager.getLogger(LoginManager.class);

    private Hashtable<String, Player> players;

    public LoginManager() {
        players = new Hashtable<String, Player>();
    }

    public boolean hasPlayer(String uuid) {
        return players.containsKey(uuid);
    }

    public boolean addPlayer(String uuid, Player player) {
        players.put(uuid, player);

        return true;
    }

    public boolean delPlayer(String uuid) {
        if (players.remove(uuid) != null) {
            LOGGER.info("player " + uuid + " exit.");
        }

        return true;
    }

    public void destroy() {
        Enumeration<Player> enumeration = players.elements();

        while (enumeration.hasMoreElements()) {
            Player player = enumeration.nextElement();
            player.release();
        }
    }

    public void printPlayers() {
        Enumeration<Player> enumeration = players.elements();

        while (enumeration.hasMoreElements()) {
            Player player = enumeration.nextElement();
            System.out.println(player.getUuid());
        }
    }

    public int getPlayersNum() {
        return players.size();
    }
}

