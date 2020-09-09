package it.polimi.middleware.akkaProject.messages;

import java.io.Serializable;

public class ForwardGetData implements Serializable {
    private final Serializable key;

    public ForwardGetData(Serializable key) {
        this.key = key;
    }

    public Serializable getKey() {
        return key;
    }
}
