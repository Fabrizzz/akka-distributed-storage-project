package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.SavedData;

import java.io.Serializable;

public class PutNewData implements Serializable {
    private final Serializable key;
    private final SavedData data;

    public PutNewData(Serializable key, SavedData data) {
        this.key = key;
        this.data = data;
    }

    public Serializable getKey() {
        return key;
    }

    public SavedData getData() {
        return data;
    }
}
