package it.polimi.middleware.akkaProject.master;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.polimi.middleware.akkaProject.dataStructures.MemberInfos;
import it.polimi.middleware.akkaProject.dataStructures.Partition;
import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingMembers;
import it.polimi.middleware.akkaProject.messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class MasterActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private Cluster cluster = Cluster.get(getContext().system());

    private List<PartitionRoutingMembers> partitionRoutingMembers; //per ogni partizione mi dice quali server la hanno e chi è il leader

    private HashMap<Member, MemberInfos> membersHashMap = new HashMap<>();

    ArrayList<MemberInfos> orderedMemberInfos;

    int timeoutMultiplier = 0;

    private int numberOfPartitions;
    private int numberOfReplicas;

    public MasterActor(int numberOfPartitions, int numberOfReplicas) {
        this.numberOfPartitions = numberOfPartitions;
        this.numberOfReplicas = numberOfReplicas;
        partitionRoutingMembers = new ArrayList<>(numberOfPartitions);

    }


    public static Props props(int numberOfPartitions, int numberOfReplicas) {
        return Props.create(MasterActor.class, numberOfPartitions, numberOfReplicas);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InitialMembers.class, this::onInitialMembers)
                .matchAny(o -> log.error("received unexpected message before initial configuration"))
                .build();
    }

    private Receive defaultBehavior() {
        return receiveBuilder()//
                .match(UpdateMemberListRequest.class, this::updateMembersListRequest)
                .match(ClusterChange.class, m -> clusterChange()) //
                .build();
    }

    private void updateUpMembers(){

        HashSet<Member> currentlyUpMembers = new HashSet<>();
        ArrayList<Member> newMembers = new ArrayList<>();


        cluster.state().getMembers().forEach(member -> {
            if (member.status().equals(MemberStatus.up()) && (member.hasRole("server"))) {
                currentlyUpMembers.add(member);
                if (!membersHashMap.containsKey(member)){
                    newMembers.add(member);
                    log.info("New member added: " + member.address());
                }
            }
        });

        ArrayList<Member> membersToRemove = new ArrayList<>();

        for (Member newMember : newMembers) {
            MemberInfos currentMemberInfos = new MemberInfos(newMember);
            try {
                Future<ActorRef> reply = getContext().actorSelection(newMember.address() + "/user/supervisor").resolveOne(new Timeout(Duration.create(1, TimeUnit.SECONDS)));
                ActorRef supervisor = Await.result(reply, Duration.Inf());
                currentMemberInfos.setSupervisorReference(supervisor);
                membersHashMap.put(newMember, currentMemberInfos);
                orderedMemberInfos.add(currentMemberInfos);
            } catch (Exception e) {
                log.warning("Unable to retrieve supervisor ActorRef of new Node: " + newMember.address());
                membersToRemove.add(newMember);

            }
        }

        currentlyUpMembers.removeAll(membersToRemove);

        ArrayList<Member> deadMembers = new ArrayList<>();
        for (Member member : membersHashMap.keySet()) {
            if (!currentlyUpMembers.contains(member)) {
                deadMembers.add(member);
                orderedMemberInfos.remove(membersHashMap.get(member));
                log.info("Member detected as offline: " + member.address());
            }
        }

        for (Member deadMember : deadMembers) {
            for (MemberInfos.PartitionInfo partitionInfo : membersHashMap.get(deadMember).getPartitionInfos()) {
                PartitionRoutingMembers curr = partitionRoutingMembers.get(partitionInfo.getPartitionId());
                if (curr.getLeader().equals( deadMember))
                    curr.setLeader(null);
                curr.getReplicas().remove(deadMember);
            }

            membersHashMap.remove(deadMember);

        }
        //todo comunico subito cambiamento partitionRoutingInfos?

        Collections.sort(orderedMemberInfos);
    }

    private void clusterChange() {

        updateUpMembers();
        timeoutMultiplier++;

        if (membersHashMap.size() < numberOfReplicas) {
            log.error("I don't have enough nodes in the cluster to relocate. Shutting down the system");
            getContext().system().terminate();
            return;
        }

        Queue<Integer> partitionsToRelocate = new LinkedList<>();

        for (PartitionRoutingMembers partition : partitionRoutingMembers) {
            if (partition.getReplicas().size() < numberOfReplicas) {
                partitionsToRelocate.add(partition.getPartitionId());
            }

        }

        while (!partitionsToRelocate.isEmpty()){
            int partitionId = partitionsToRelocate.poll();
            log.info("Starting relocation of partition: " + partitionId);
            List<Member> replicaMembers = partitionRoutingMembers.get(partitionId).getReplicas();
            Member leader = partitionRoutingMembers.get(partitionId).getLeader();

            if (replicaMembers.isEmpty()){
                log.error("All the replicas of Partition " + partitionId + " are dead, shutting down the system");
                getContext().system().terminate();
                return;
            }

            Partition snapshot = null;
            if (leader != null) {
                try {
                    Future<Object> reply = Patterns.ask(membersHashMap.get(leader).getSupervisorReference(), new SnapshotReplicaRequest(partitionId), 1000 * timeoutMultiplier);
                    snapshot = ((SnapshotReplica) Await.result(reply, Duration.Inf())).getReplica();
                    partitionRoutingMembers.get(partitionId).setLeader(null);
                    membersHashMap.get(leader).getPartitionInfos().stream().filter(k -> k.getPartitionId() == partitionId).findAny().get().setIAmLeader(false);
                    log.info("Just obtained the snapshot of Partition " + partitionId + "from leader " + leader.address());
                } catch (Exception e) {
                    log.warning("Unable to contact the leader of Partition:" + partitionId + " on " + leader.address(), e);
                    if (timeoutMultiplier < 5)
                        getContext().system().scheduler().scheduleOnce(java.time.Duration.of(1, ChronoUnit.SECONDS), self(), new ClusterChange(), getContext().getDispatcher(), self());
                    else
                        cluster.down(leader.address());
                    return;
                }
            } else {

                for (Member replicaMember : replicaMembers) {
                    try {
                        Future<Object> reply = Patterns.ask(membersHashMap.get(replicaMember).getSupervisorReference(), new SnapshotReplicaRequest(partitionId), 1000*timeoutMultiplier);


                        Partition snapshotTemp = ((SnapshotReplica) Await.result(reply, Duration.Inf())).getReplica();
                        log.info("Just obtained the snapshot of Partition " + partitionId + "from replica " + replicaMember.address());
                        if (snapshot == null || snapshot.getState() < snapshotTemp.getState()) {
                            snapshot = snapshotTemp;
                        }
                    } catch (Exception e) {
                        log.warning("Unable to retrieve a snapshot from a member, maybe because of timeOut");
                        if (timeoutMultiplier < 5)
                            getContext().system().scheduler().scheduleOnce(java.time.Duration.of(1, ChronoUnit.SECONDS), self(), new ClusterChange(), getContext().getDispatcher(), self());
                        else
                            cluster.down(replicaMember.address());
                        return;
                    }
                }

            }

            if (snapshot == null) {
                log.error("Unable to retrieve ANY snapshot for this Replica");
                return;
            } else {
                ArrayList<Member> newPartitionMembers = new ArrayList<>();
                Collections.sort(orderedMemberInfos);
                for (MemberInfos member : orderedMemberInfos) {
                    if (replicaMembers.size() + newPartitionMembers.size() >= numberOfReplicas)
                        break;
                    if (!replicaMembers.contains(member.getMember()))
                        newPartitionMembers.add(member.getMember());
                }

                for (Member newPartitionMember : newPartitionMembers) {
                    try {
                    Future<Object> reply = Patterns.ask(membersHashMap.get(newPartitionMember).getSupervisorReference(),new AllocateLocalPartition(snapshot), 1000 * timeoutMultiplier);
                    Await.result(reply, Duration.Inf());
                    membersHashMap.get(newPartitionMember).getPartitionInfos().add(new MemberInfos.PartitionInfo(partitionId, false));
                    partitionRoutingMembers.get(partitionId).getReplicas().add(newPartitionMember);
                    } catch (Exception e) {
                        log.warning("Unable to allocate Partition" + partitionId + "on " + newPartitionMember.address());
                        if (timeoutMultiplier < 5)
                            getContext().system().scheduler().scheduleOnce(java.time.Duration.of(1, ChronoUnit.SECONDS), self(), new ClusterChange(), getContext().getDispatcher(), self());
                        else
                            cluster.down(newPartitionMember.address());
                        return;
                    }
                }
                try {
                    Member newLeader = partitionRoutingMembers.get(partitionId).getReplicas().get(0);
                    List<Address> otherReplicas = partitionRoutingMembers.get(partitionId).getReplicas().stream().map(Member::address).collect(Collectors.toList());
                    otherReplicas.remove(newLeader.address());
                    Future<Object> reply = Patterns.ask(membersHashMap.get(newLeader)
                            .getSupervisorReference(), new BecomeLeader(partitionId, otherReplicas), 1000*timeoutMultiplier);
                    Object secondReply = Await.result(reply, Duration.Inf());
                    if (secondReply instanceof UnableToContactReplicas) {
                        log.warning("Unable to elect the leader of Partition "+partitionId + " on "+newLeader.address());
                        getContext().system().scheduler().scheduleOnce(java.time.Duration.of(1, ChronoUnit.SECONDS), self(), new ClusterChange(), getContext().getDispatcher(), self());
                        return;
                    }
                    else {
                        partitionRoutingMembers.get(partitionId).setLeader(newLeader);
                        membersHashMap.get(newLeader).getPartitionInfos().stream().filter(k -> k.getPartitionId() == partitionId).findAny().get().setIAmLeader(true);
                        log.info("Elected NEW leader of Partition "+partitionId + " on "+newLeader.address());
                        RoutingConfigurationUpdate update = new RoutingConfigurationUpdate(partitionId, partitionRoutingMembers.get(partitionId));
                        for (Member member : membersHashMap.keySet()) {
                            getContext().actorSelection(member.address() + "/user/routerManager").tell(update, self());
                        }
                    }
                }
                catch (Exception e){
                    log.warning("Unable to elect the leader of Partition "+partitionId + " on "+partitionRoutingMembers.get(partitionId).getReplicas().get(0).address());
                    getContext().system().scheduler().scheduleOnce(java.time.Duration.of(1, ChronoUnit.SECONDS), self(), new ClusterChange(), getContext().getDispatcher(), self());
                    return;
                }
                timeoutMultiplier = 1;
            }
        }
        log.info("RELOCATION COMPLETED");
        balanceNodes();
    }

    private void balanceNodes(){


    }

    //initial set up
    private void onInitialMembers(InitialMembers initialMembers) {
        ArrayList<Member> members = initialMembers.getInitialMembers();
        ArrayList<MemberInfos> memberInfos = new ArrayList<>();

        if (members.size() < numberOfReplicas) {
            log.error("Not enough members");
            getContext().system().terminate();
            return;
        }
        //create memberInfos for each member
        for (Member member : members) {
            memberInfos.add(new MemberInfos(member));
        }



        //todo ask per allocate Local Partition, altrimenti il successivo becomeLeader potrebbe fallire
        int currentMember = 0;
        for (int partitionId = 0; partitionId < numberOfPartitions; partitionId++) {
            partitionRoutingMembers.add(new PartitionRoutingMembers(partitionId, members.get(currentMember % members.size()))); //create partitionRoutingInfo and set the leader
            for (int j = 0; j < numberOfReplicas; j++) {
                partitionRoutingMembers.get(partitionId).getReplicas().add(members.get(currentMember % members.size()));
                if (j == 0)
                    memberInfos.get(currentMember % members.size()).getPartitionInfos().add(new MemberInfos.PartitionInfo(partitionId, true));
                else
                    memberInfos.get(currentMember % members.size()).getPartitionInfos().add(new MemberInfos.PartitionInfo(partitionId, false));
                try {
                    Future<ActorRef> reply = getContext().actorSelection(members.get(currentMember % members.size()).address() + "/user/supervisor").resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
                    ActorRef supervisor = Await.result(reply, Duration.Inf());
                    memberInfos.get(currentMember % members.size()).setSupervisorReference(supervisor);

                    Future<Object> secondReply = Patterns.ask(supervisor, new AllocateLocalPartition(new Partition(partitionId)), 1000);
                    Await.result(secondReply, Duration.Inf());

                    log.info("Allocated partition " + partitionId + " on member: " + memberInfos.get(currentMember % members.size()).getMember().address());
                } catch (Exception e) {
                    log.error("Couldn't allocate Partition: " + partitionId + " on: " + memberInfos.get(currentMember % members.size()).getMember().address(), e);
                    getContext().system().terminate();
                    return;
                }
                currentMember++;
            }
        }

        for (MemberInfos memberInfo : memberInfos) {
            membersHashMap.put(memberInfo.getMember(), memberInfo);
        }

        for (MemberInfos memberInfo : memberInfos) {
            for (MemberInfos.PartitionInfo partition : memberInfo.getPartitionInfos()) {
                if (partition.iAmLeader()) {
                    ArrayList<Address> otherReplicas = new ArrayList<>();
                    for (Member replica : partitionRoutingMembers.get(partition.getPartitionId()).getReplicas()) {
                        if (!memberInfo.getMember().equals(replica))
                            otherReplicas.add(replica.address());
                    }
                    try {
                        Future<Object> reply = Patterns.ask(memberInfo.getSupervisorReference(), (new BecomeLeader(partition.getPartitionId(), otherReplicas)), 3000);
                        if (Await.result(reply, Duration.Inf()) instanceof UnableToContactReplicas) {
                            log.error("Unable to elect a leader, because the leader couldn't contact other replicas");
                            getContext().system().terminate();
                            return;
                        }
                        log.info("Elected leader of partition " + partition.getPartitionId() + " on member " + memberInfo.getMember().address());

                    } catch (Exception e) {
                        log.error("Couldn't elect Leader, because he doesn't answer in time");
                        getContext().system().terminate();
                        return;

                    }
                }
            }
            RoutingConfigurationMessage configuration = new RoutingConfigurationMessage(partitionRoutingMembers);
            getContext().actorSelection(memberInfo.getMember().address() + "/user/routerManager").tell(configuration, self());
        }
        orderedMemberInfos = new ArrayList<>(memberInfos);
        Collections.sort(orderedMemberInfos);
        getContext().become(defaultBehavior());
        log.info("Set up Completed");
    }

    private void updateMembersListRequest(UpdateMemberListRequest message){
        ArrayList<Address> list = new ArrayList<>();
        for (Member member : cluster.state().getMembers()) {
            if (member.status().equals(MemberStatus.up()) && member.hasRole("server"))
                list.add(member.address());
        }
        sender().tell(new UpdateMemberListAnswer(list), self());
    }

    //non fa niente
    @Override
    public void preStart() {
        System.out.println("I started "  + getContext().getSelf().path());
    }

    //non fa niente
    @Override
    public void postStop(){

        System.out.println("I am dead: " + getContext().getSelf().path());
        getContext().system().terminate();

    }

    //unwatcha e uccide i figli e chiama postStop
    @Override
    public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
        System.out.println("I am restarting " + getContext().getSelf().path());
        log.error(
                reason,
                "Restarting due to [{}] when processing [{}]",
                reason.getMessage(),
                message.orElse(""));
        super.preRestart(reason,message);
    }

    //chiama preStart
    @Override
    public void postRestart(Throwable reason) throws Exception {
        System.out.println("I restarted " + getContext().getSelf().path());
        super.postRestart(reason);
    }
}
