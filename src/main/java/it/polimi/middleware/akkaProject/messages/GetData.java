package it.polimi.middleware.akkaProject.messages;

import java.io.Serializable;
import java.time.ZonedDateTime;

public class GetData implements Serializable {
    private final Serializable key;
    private final ZonedDateTime generatedAtLeastAt;

    public GetData(Serializable key, ZonedDateTime generatedAtLeastAt) {
        this.key = key;
        this.generatedAtLeastAt = generatedAtLeastAt;
    }

    public Serializable getKey() {
        return key;
    }

    public ZonedDateTime getGeneratedAtLeastAt() {
        return generatedAtLeastAt;
    }
}
