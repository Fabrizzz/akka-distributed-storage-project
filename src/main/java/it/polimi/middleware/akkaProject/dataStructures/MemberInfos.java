package it.polimi.middleware.akkaProject.dataStructures;

import akka.actor.ActorRef;
import akka.cluster.Member;

import java.io.Serializable;
import java.util.ArrayList;

/**this class stores the List of Partitions managed by the member
 */
public class MemberInfos implements Serializable, Comparable<MemberInfos> {
    private final Member member;
    private final ArrayList<PartitionInfo> partitionInfos = new ArrayList<>();
    private ActorRef supervisorReference;

    public MemberInfos(Member member) {
        this.member = member;
    }

    public Member getMember() {
        return member;
    }

    public ArrayList<PartitionInfo> getPartitionInfos() {
        return partitionInfos;
    }

    public ActorRef getSupervisorReference() {
        return supervisorReference;
    }

    public void setSupervisorReference(ActorRef supervisorReference) {
        this.supervisorReference = supervisorReference;
    }

    public int getSize(){
        return partitionInfos.size();
    }

    @Override
    public int compareTo(MemberInfos o) {
        return this.partitionInfos.size() - o.partitionInfos.size();
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

        public void setPartitionId(int partitionId) {
            this.partitionId = partitionId;
        }

        public void setIAmLeader(boolean iAmLeader) {
            this.iAmLeader = iAmLeader;
        }
    }


}
