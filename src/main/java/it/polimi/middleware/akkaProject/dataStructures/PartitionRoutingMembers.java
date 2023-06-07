package it.polimi.middleware.akkaProject.dataStructures;

import akka.cluster.Member;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class stores the addresses of all the Replicas of a Partition
 */
public class PartitionRoutingMembers implements Serializable {
    private final int partitionId;
    private List<Member> replicas = new ArrayList<>(); //leader included
    private Member leader; //può essere null

    public PartitionRoutingMembers(int partitionId, Member leader) {
        this.partitionId = partitionId;
        this.leader = leader;
    }

    public PartitionRoutingMembers(int partitionId) {
        this.partitionId = partitionId;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public List<Member> getReplicas() {
        return replicas;
    }

    public void setReplicas(List<Member> replicas) {
        this.replicas = replicas;
    }

    public Member getLeader() {
        return leader;
    }

    public void setLeader(Member leader) {
        this.leader = leader;
    }
}
