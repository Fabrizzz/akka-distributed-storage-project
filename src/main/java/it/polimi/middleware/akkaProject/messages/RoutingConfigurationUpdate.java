package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingMembers;

import java.io.Serializable;

public class RoutingConfigurationUpdate implements Serializable {
    private final int partitionId;
    private final PartitionRoutingMembers partitionRoutingMembers;

    public RoutingConfigurationUpdate(int partitionId, PartitionRoutingMembers partitionRoutingMembers) {
        this.partitionId = partitionId;
        this.partitionRoutingMembers = partitionRoutingMembers;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public PartitionRoutingMembers getPartitionRoutingMembers() {
        return partitionRoutingMembers;
    }
}
