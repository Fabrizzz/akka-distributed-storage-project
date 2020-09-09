package it.polimi.middleware.akkaProject.dataStructures;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**this class is used to store Data with the timestamp when it was generated
 */
public class DataWithTimestamp implements Serializable {
    private final Serializable data;
    private final ZonedDateTime generatedAt;

    public DataWithTimestamp(Serializable data, ZonedDateTime generatedAt) {
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
