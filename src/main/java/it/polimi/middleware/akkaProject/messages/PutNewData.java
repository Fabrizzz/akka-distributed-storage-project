package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.DataWithTimestamp;

import java.io.Serializable;

public class PutNewData implements Serializable {
    private final Serializable key;
    private final DataWithTimestamp data;

    public PutNewData(Serializable key, DataWithTimestamp data) {
        this.key = key;
        this.data = data;
    }

    public Serializable getKey() {
        return key;
    }

    public DataWithTimestamp getData() {
        return data;
    }
}
