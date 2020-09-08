package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.Partition;

import java.io.Serializable;

public class SnapshotReplica implements Serializable {

    private final Partition replica;


    public SnapshotReplica(Partition replica) {
        this.replica = replica;
    }

    public Partition getReplica() {
        return replica;
    }
}
