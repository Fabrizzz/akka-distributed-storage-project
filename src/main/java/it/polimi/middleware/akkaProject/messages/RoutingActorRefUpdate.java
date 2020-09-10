package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingActorRefs;

import java.io.Serializable;

public class RoutingActorRefUpdate implements Serializable {
    private final int partitionId;
    private final PartitionRoutingActorRefs list;


    public RoutingActorRefUpdate(int partitionId, PartitionRoutingActorRefs list) {
        this.partitionId = partitionId;
        this.list = list;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public PartitionRoutingActorRefs getList() {
        return list;
    }
}
