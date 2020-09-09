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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;


public class MasterActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    Cluster cluster = Cluster.get(getContext().system());
    List<PartitionRoutingAddresses> partitionRoutingAddresses; //per ogni partizione mi dice quali server la hanno e chi è il leader
    TreeSet<MemberInfos> memberInfos;


    int numberOfPartitions;
    int numberOfReplicas;

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
                .match(UpdateMemberListRequest.class, this::updateMembersListRequest)
                .matchAny(o -> log.error("received unexpected message before initial configuration"))
                .build();
    }

    private void updateMembersListRequest(UpdateMemberListRequest message){
        ArrayList<Address> list = new ArrayList<>();
        for (Member member : cluster.state().getMembers()) {
            if (member.status().equals(MemberStatus.up()) && member.hasRole("server"))
                list.add(member.address());
        }
        sender().tell(new UpdateMemberListAnswer(list), self());
    }

    //initial set up
    private void onInitialMembers(InitialMembers initialMembers){
        ArrayList<Member> members = initialMembers.getInitialMembers();
        ArrayList<MemberInfos> memberInfos = new ArrayList<>();

        //create memberInfos for each member
        for (Member member : members) {
            memberInfos.add(new MemberInfos(member));
        }

        if (members.size() < numberOfReplicas) {
            log.error("Not enough members");
            getContext().system().terminate();
        }

        else {
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
                    Future<ActorRef> reply = getContext().actorSelection(members.get(currentMember % members.size()).address() + "/user/supervisor").resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
                    try {
                        ActorRef supervisor = Await.result(reply, Duration.Inf());
                        memberInfos.get(currentMember % members.size()).setSupervisorReference(supervisor);

                        Future<Object> secondReply = Patterns.ask(supervisor, new AllocateLocalPartition(new Partition(partitionId)), 1000);
                        Await.result(secondReply, Duration.Inf());

                    }catch (Exception e) {
                        log.error("Couldn't allocate Partition: " + partitionId + " on: " + memberInfos.get(currentMember % members.size()).getMember().address(), e);
                        getContext().system().terminate();
                    }
                    currentMember++;
                }
            }

            this.memberInfos = new TreeSet<>(memberInfos);

            for (MemberInfos memberInfo : memberInfos) {
                for (MemberInfos.PartitionInfo partition : memberInfo.getPartitionInfos()) {
                    if (partition.iAmLeader()){
                        ArrayList<Address> otherReplicas = new ArrayList<>();
                        for (Address replica : partitionRoutingAddresses.get(partition.getPartitionId()).getReplicas()) {
                            if (!memberInfo.getMember().address().equals(replica))
                                otherReplicas.add(replica);
                        }
                        try {
                            Future<Object> reply = Patterns.ask(memberInfo.getSupervisorReference(), (new BecomeLeader(partition.getPartitionId(),otherReplicas)), 1000);
                            if(Await.result(reply, Duration.Inf()) instanceof UnableToContactReplicas){
                                log.error("Unable to elect a leader, because the leader couldn't contact other replicas");
                                getContext().system().terminate();
                            }

                        } catch (Exception e) {
                            log.error("Couldn't elect Leader, because he doesn't answer in time");
                            getContext().system().terminate();

                        }

                    }
                }

                RoutingConfigurationMessage configuration = new RoutingConfigurationMessage(partitionRoutingAddresses);
                getContext().actorSelection(memberInfo.getMember().address() + "/user/routerManager").tell(configuration, self());
            }

            


            //todo change behavior per accettere futuri messaggi
        }
    }

    //todo funzione magica che redistribuisce le partizioni


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
