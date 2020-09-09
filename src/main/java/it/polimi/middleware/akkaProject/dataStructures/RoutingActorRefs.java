package it.polimi.middleware.akkaProject.dataStructures;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Address;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoutingActorRefs implements Serializable {
    private final int partitionId;
    private List<ActorRef> replicas = new ArrayList<>(); //leader included
    private ActorRef leader;

    public RoutingActorRefs(int partitionId) {
        this.partitionId = partitionId;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public List<ActorRef> getReplicas() {
        return replicas;
    }

    public ActorRef getLeader() {
        return leader;
    }

    public void setLeader(ActorRef leader) {
        this.leader = leader;
    }
}
