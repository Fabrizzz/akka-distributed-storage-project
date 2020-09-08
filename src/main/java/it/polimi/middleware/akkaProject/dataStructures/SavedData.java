package it.polimi.middleware.akkaProject.dataStructures;

import java.io.Serializable;
import java.time.ZonedDateTime;

public class SavedData implements Serializable {
    private final Serializable data;
    private final ZonedDateTime generatedAt;

    public SavedData(Serializable data, ZonedDateTime generatedAt) {
        this.data = data;
        this.generatedAt = generatedAt;
    }

    public Serializable getData() {
        return data;
    }

    public ZonedDateTime getGeneratedAt() {
        return generatedAt;
    }
}
