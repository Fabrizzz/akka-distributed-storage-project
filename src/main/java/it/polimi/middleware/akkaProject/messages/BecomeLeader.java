package it.polimi.middleware.akkaProject.messages;

import akka.actor.Address;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BecomeLeader implements Serializable {
    private final int partitionId;
    private final ArrayList<Address> otherReplicas;


    public BecomeLeader(int partitionId, List<Address> otherReplicas) {
        this.partitionId = partitionId;
        this.otherReplicas = new ArrayList<>(otherReplicas);
    }

    public int getPartitionId() {
        return partitionId;
    }

    public ArrayList<Address> getOtherReplicas() {
        return otherReplicas;
    }
}
