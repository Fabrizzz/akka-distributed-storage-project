package it.polimi.middleware.akkaProject.messages;

import java.io.Serializable;

public class SnapshotReplicaRequest implements Serializable {
    private final int partitionId;

    public SnapshotReplicaRequest(int partitionId) {
        this.partitionId = partitionId;
    }

    public int getPartitionId() {
        return partitionId;
    }
}
