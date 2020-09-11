package it.polimi.middleware.akkaProject.messages;

import java.io.Serializable;

public class DeletePartition implements Serializable {

    private final int partitionId;

    public DeletePartition(int partitionId) {
        this.partitionId = partitionId;
    }

    public int getPartitionId() {
        return partitionId;
    }
}
