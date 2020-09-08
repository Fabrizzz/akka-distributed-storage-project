package it.polimi.middleware.akkaProject.master;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.Member;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.polimi.middleware.akkaProject.dataStructures.MemberInfos;
import it.polimi.middleware.akkaProject.dataStructures.Partition;
import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingInfo;
import it.polimi.middleware.akkaProject.messages.AllocateLocalPartition;
import it.polimi.middleware.akkaProject.messages.BecomeLeader;
import it.polimi.middleware.akkaProject.messages.InitialRoutingConfiguration;
import it.polimi.middleware.akkaProject.messages.InitialMembers;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MasterActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    Cluster cluster = Cluster.get(getContext().system());
    List<PartitionRoutingInfo> partitionRoutingInfos; //per ogni partizione mi dice quali server la hanno e chi è il leader
    TreeSet<MemberInfos> memberInfos;


    int numberOfPartitions;
    int numberOfReplicas;

    public MasterActor(int numberOfPartitions, int numberOfReplicas) {
        this.numberOfPartitions = numberOfPartitions;
        this.numberOfReplicas = numberOfReplicas;
        partitionRoutingInfos = new ArrayList<>(numberOfPartitions);

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

    //initial set up
    private void onInitialMembers(InitialMembers initialMembers) throws TimeoutException, InterruptedException {
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
                partitionRoutingInfos.add(new PartitionRoutingInfo(partitionId, members.get(currentMember % members.size()).address())); //create partitionRoutingInfo and set the leader
                for (int j = 0; j < numberOfReplicas; j++) {
                    partitionRoutingInfos.get(partitionId).getReplicas().add(members.get(currentMember % members.size()).address());
                    if (j == 0)
                        memberInfos.get(currentMember % members.size()).getPartitions().add(new MemberInfos.PartitionInfo(partitionId, true));
                    else
                        memberInfos.get(currentMember % members.size()).getPartitions().add(new MemberInfos.PartitionInfo(partitionId, false));
                    Future<ActorRef> reply = getContext().actorSelection(members.get(currentMember % members.size()).address() + "/user/supervisor").resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
                    ActorRef supervisor;
                    try {
                        supervisor = Await.result(reply, Duration.Inf());
                        memberInfos.get(currentMember % members.size()).setSupervisor(supervisor);
                    } catch (Exception e) {
                        log.error("Couldn't retrieve the ActorRef of a Node", e);
                        throw e;
                    }
                    Future<Object> secondReply = Patterns.ask(supervisor, new AllocateLocalPartition(new Partition(partitionId)), 1000);
                    try {
                        Await.result(secondReply, Duration.Inf());

                    } catch (Exception e) {
                        log.error("Couldn't retrieve the ActorRef of a Partition", e);
                        throw e;
                    }
                    currentMember++;
                }
            }

            this.memberInfos = new TreeSet<>(memberInfos);
            InitialRoutingConfiguration configuration = new InitialRoutingConfiguration(partitionRoutingInfos);

            for (MemberInfos memberInfo : memberInfos) {
                for (MemberInfos.PartitionInfo partition : memberInfo.getPartitions()) {
                    if (partition.iAmLeader()){
                        ArrayList<Address> otherReplicas = new ArrayList<>();
                        for (Address replica : partitionRoutingInfos.get(partition.getPartitionId()).getReplicas()) {
                            if (!memberInfo.getMember().address().equals(replica))
                                otherReplicas.add(replica);
                        }
                        getContext().actorSelection(memberInfo.getMember().address() + "/user/supervisor/partition"+partition.getPartitionId()).tell(new BecomeLeader(otherReplicas), self());
                    }
                }
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
