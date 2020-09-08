package it.polimi.middleware.akkaProject.dataStructures;

import akka.actor.ActorRef;
import akka.cluster.Member;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MemberInfos implements Serializable, Comparable<MemberInfos> {
    private final Member member;
    private ArrayList<PartitionInfo> partitions = new ArrayList<>();
    private ActorRef supervisor;

    public MemberInfos(Member member) {
        this.member = member;
    }

    public Member getMember() {
        return member;
    }

    public ArrayList<PartitionInfo> getPartitions() {
        return partitions;
    }

    public ActorRef getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(ActorRef supervisor) {
        this.supervisor = supervisor;
    }

    @Override
    public int compareTo(MemberInfos o) {
        return this.partitions.size() - o.partitions.size();
    }

    public static class PartitionInfo{
        private int partitionId;
        private boolean iAmLeader = false;

        public PartitionInfo(int partitionId, boolean iAmLeader) {
            this.partitionId = partitionId;
            this.iAmLeader = iAmLeader;
        }

        public int getPartitionId() {
            return partitionId;
        }

        public boolean iAmLeader() {
            return iAmLeader;
        }
    }


}
