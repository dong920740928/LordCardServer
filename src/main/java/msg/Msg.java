package msg;

import java.io.Serializable;

/**
 * Created by dong on 13/07/2017.
 */
public abstract class Msg implements Serializable{
    protected String action;

    public Msg(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
