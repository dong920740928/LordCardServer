package server;

import model.Game;
import model.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by yizhe on 17-7-20.
 */
public class GameManager {
    private static final Logger LOGGER = LogManager.getLogger(GameManager.class);

    private Hashtable<String, Game> player2Games;

    public GameManager() {
        player2Games = new Hashtable<String, Game>();
    }

    public void addGame(Game game) {
        List<Player> players = game.players;
        for (int i = 0; i < 3; ++i) {
            Player player = players.get(i);
            player2Games.put(player.getUuid(), game);
        }
    }

    public void delGame(Game game) {
        List<Player> players = game.players;
        for (int i = 0; i < 3; ++i) {
            Player player = players.get(i);
            player2Games.remove(player.getUuid());
            if (player.changeState(Player.State.PLAYING, Player.State.ONLINE)) {
                LOGGER.info("player " + player.getUuid() + "game end");
            } else {
                LOGGER.error("player " + player.getUuid() + "game end error");
            }
        }
    }

    public int isPlaying(String uuid) {
        return player2Games.get(uuid) == null ? -1 : 1;
    }

    public Game getGame(String uuid) {
        return player2Games.get(uuid);
    }

    public int getGamesNum() {
        return player2Games.size();
    }
}
