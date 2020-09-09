package it.polimi.middleware.akkaProject.dataStructures;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Address;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** This class stores the actorRef of all the Replicas of a Partition*/

public class PartitionRoutingActorRefs implements Serializable {
    private final int partitionId;
    private List<ActorRef> replicas = new ArrayList<>(); //leader included
    private ActorRef leader; //it can be null is the routerManager wasn't able to contact it

    public PartitionRoutingActorRefs(int partitionId) {
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
