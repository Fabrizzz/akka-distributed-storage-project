package it.polimi.middleware.akkaProject.master;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.polimi.middleware.akkaProject.dataStructures.MemberInfos;
import it.polimi.middleware.akkaProject.dataStructures.Partition;
import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingAddresses;
import it.polimi.middleware.akkaProject.messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class MasterActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private Cluster cluster = Cluster.get(getContext().system());
    private List<PartitionRoutingAddresses> partitionRoutingAddresses; //per ogni partizione mi dice quali server la hanno e chi è il leader
    private HashMap<Member, MemberInfos> memberHashMap = new HashMap<>();

    private int numberOfPartitions;
    private int numberOfReplicas;

    public MasterActor(int numberOfPartitions, int numberOfReplicas) {
        this.numberOfPartitions = numberOfPartitions;
        this.numberOfReplicas = numberOfReplicas;
        partitionRoutingAddresses = new ArrayList<>(numberOfPartitions);

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

    private void clusterChange(){
        HashSet<Member> currentlyUpMembers = new HashSet<>();
        ArrayList<Member> newMembers = new ArrayList<>();
        ArrayList<Member> deadMembers = new ArrayList<>();
        ArrayList<MemberInfos> sortedMemberInfos = new ArrayList<>(memberHashMap.values());
        LinkedList<Integer> partitionsToRelocate = new LinkedList<>();



        cluster.state().getMembers().forEach(member -> {
            if (member.status().equals(MemberStatus.up()) && (member.hasRole("server"))) {
                currentlyUpMembers.add(member);
                if (!memberHashMap.containsKey(member)){
                    newMembers.add(member);
                    log.info("New member added: " + member.address());
                }
            }
        });
        for (Member member : memberHashMap.keySet()) {
            if (!currentlyUpMembers.contains(member)) {
                deadMembers.add(member);
                log.info("Member detected as offline: " + member.address());
            }
        }

        if (currentlyUpMembers.size() < numberOfReplicas){
            log.error("I don't have enough nodes in the cluster to relocate. Shutting down the system");
            getContext().system().terminate();
            return;
        }

        for (int i = 0; i < newMembers.size(); i++) {
            Member newMember = newMembers.get(i);
            MemberInfos currentMemberInfos = new MemberInfos(newMember);
            boolean obtainedActorRef = false;
            int timeout = 1;

            while (!obtainedActorRef) {
                try {
                    Future<ActorRef> reply = getContext().actorSelection(newMember.address() + "/user/supervisor").resolveOne(new Timeout(scala.concurrent.duration.Duration.create(timeout++, TimeUnit.SECONDS)));
                    ActorRef supervisor = Await.result(reply, Duration.Inf());
                    currentMemberInfos.setSupervisorReference(supervisor);
                    obtainedActorRef = true;
                    sortedMemberInfos.add(currentMemberInfos);
                    memberHashMap.put(newMember, currentMemberInfos);
                } catch (Exception e) {
                    log.warning("Unable to retrieve supervisor ActorRef of new Node: " + newMember.address());
                    if (cluster.state().members().find(m -> m.equals(newMember) && m.status().equals(MemberStatus.up())).isEmpty()){
                        newMembers.remove(i);
                        i--;
                        obtainedActorRef = true;
                        log.warning("The newMember immediatly went offline");
                    }

                }
            }
        }

        Collections.sort(sortedMemberInfos);



        while (!partitionsToRelocate.isEmpty() ||
                sortedMemberInfos.get(sortedMemberInfos.size()-1).getSize() > 1+sortedMemberInfos.get(0).getSize()){

            if (!partitionsToRelocate.isEmpty()){

                int currentPartitionId = partitionsToRelocate.getFirst();
                //partitionRoutingAddresses.get(currentPartitionId).getReplicas()

                
            }
            else{

            }
        }


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
            partitionRoutingAddresses.add(new PartitionRoutingAddresses(partitionId, members.get(currentMember % members.size()).address())); //create partitionRoutingInfo and set the leader
            for (int j = 0; j < numberOfReplicas; j++) {
                partitionRoutingAddresses.get(partitionId).getReplicas().add(members.get(currentMember % members.size()).address());
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

                } catch (Exception e) {
                    log.error("Couldn't allocate Partition: " + partitionId + " on: " + memberInfos.get(currentMember % members.size()).getMember().address(), e);
                    getContext().system().terminate();
                    return;
                }
                currentMember++;
            }
        }

        for (MemberInfos memberInfo : memberInfos) {
            memberHashMap.put(memberInfo.getMember(), memberInfo);
        }

        for (MemberInfos memberInfo : memberInfos) {
            for (MemberInfos.PartitionInfo partition : memberInfo.getPartitionInfos()) {
                if (partition.iAmLeader()) {
                    ArrayList<Address> otherReplicas = new ArrayList<>();
                    for (Address replica : partitionRoutingAddresses.get(partition.getPartitionId()).getReplicas()) {
                        if (!memberInfo.getMember().address().equals(replica))
                            otherReplicas.add(replica);
                    }
                    try {
                        Future<Object> reply = Patterns.ask(memberInfo.getSupervisorReference(), (new BecomeLeader(partition.getPartitionId(), otherReplicas)), 3000);
                        if (Await.result(reply, Duration.Inf()) instanceof UnableToContactReplicas) {
                            log.error("Unable to elect a leader, because the leader couldn't contact other replicas");
                            getContext().system().terminate();
                            return;
                        }

                    } catch (Exception e) {
                        log.error("Couldn't elect Leader, because he doesn't answer in time");
                        getContext().system().terminate();
                        return;

                    }
                }
            }
            RoutingConfigurationMessage configuration = new RoutingConfigurationMessage(partitionRoutingAddresses);
            getContext().actorSelection(memberInfo.getMember().address() + "/user/routerManager").tell(configuration, self());
        }
        getContext().become(defaultBehavior());
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
