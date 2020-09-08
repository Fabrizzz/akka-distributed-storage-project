package it.polimi.middleware.akkaProject.messages;

import java.io.Serializable;

public class GetPutUpdate implements Serializable {
    private final PutNewData message;
    private final int state;

    public GetPutUpdate(PutNewData message, int state) {
        this.message = message;
        this.state = state;
    }

    public PutNewData getMessage() {
        return message;
    }

    public int getState() {
        return state;
    }
}
