package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.SavedData;

import java.io.Serializable;

public class DataReply implements Serializable {
    private final SavedData data;

    public DataReply(SavedData data) {
        this.data = data;
    }

    public SavedData getData() {
        return data;
    }
}
