package it.polimi.middleware.akkaProject.dataStructures;

import akka.actor.ActorSelection;
import akka.actor.Address;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** This class stores the addresses of all the Replicas of a Partition*/
public class PartitionRoutingAddresses implements Serializable {
    private final int partitionId;
    private List<Address> replicas = new ArrayList<>(); //leader included
    private Address leader;

    public PartitionRoutingAddresses(int partitionId, Address leader) {
        this.partitionId = partitionId;
        this.leader = leader;
    }

    public PartitionRoutingAddresses(int partitionId) {
        this.partitionId = partitionId;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public List<Address> getReplicas() {
        return replicas;
    }

    public void setReplicas(List<Address> replicas) {
        this.replicas = replicas;
    }

    public Address getLeader() {
        return leader;
    }

    public void setLeader(Address leader) {
        this.leader = leader;
    }
}
