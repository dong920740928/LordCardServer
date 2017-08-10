package poker;

/**
 * Created by yizhe on 17-7-17.
 */
public class Poker {
    public int id;
    public int figure;
    public int pattern;
    public boolean valid;

    public Poker(int id) {
        this.id = id;
        this.figure = (id & 0xff0) >> 4;
        this.pattern = id & 0x00f;
        this.valid = false;
        if (figure >= 0x03 && figure <= 0x11) {
            if (pattern <= 0x005) {
                this.valid = true;
            }
        }
    }
}
