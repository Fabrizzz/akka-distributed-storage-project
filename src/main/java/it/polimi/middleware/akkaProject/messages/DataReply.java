package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.DataWithTimestamp;

import java.io.Serializable;

public class DataReply implements Serializable {
    private final DataWithTimestamp data;

    public DataReply(DataWithTimestamp data) {
        this.data = data;
    }

    public DataWithTimestamp getData() {
        return data;
    }
}
