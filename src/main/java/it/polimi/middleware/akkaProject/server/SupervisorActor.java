package it.polimi.middleware.akkaProject.server;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.japi.pf.DeciderBuilder;
import it.polimi.middleware.akkaProject.messages.*;
import scala.concurrent.duration.Duration;


import java.util.Optional;


public class SupervisorActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    Cluster cluster = Cluster.get(getContext().system());
    ActorRef[] localPartitions;

    public SupervisorActor(int numberOfPartitions) {
        this.localPartitions = new ActorRef[numberOfPartitions];
    }

    public static Props props(int numberOfPartitions) {
        return Props.create(SupervisorActor.class, numberOfPartitions);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AllocateLocalPartition.class, this::allocateLocalPartition)
                .match(BecomeLeader.class, this::becomeLeader)
                .match(SnapshotReplicaRequest.class, this::snapshotReplicaRequest)
                .match(DeletePartition.class, this::deletePartition)
                .matchAny(o -> log.error("received unknown message"))
                .build();
    }

    public void deletePartition(DeletePartition message){
        int partitionId = message.getPartitionId();
        if (localPartitions[partitionId] != null)
            localPartitions[partitionId].tell(akka.actor.PoisonPill.getInstance(), self());
        localPartitions[partitionId] = null;
    }


    private void snapshotReplicaRequest(SnapshotReplicaRequest message){
        ActorRef current = localPartitions[message.getPartitionId()];
        if (current != null)
            current.forward(message, getContext());
    }

    private void becomeLeader(BecomeLeader message){
        if (localPartitions[message.getPartitionId()] != null)
            localPartitions[message.getPartitionId()].forward(message, getContext());
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new AllForOneStrategy(//
                10, //
                Duration.create("10 seconds"), //
                DeciderBuilder //
                        .match(Exception.class, ex -> SupervisorStrategy.escalate()) //
                        .build());
    }


    private void allocateLocalPartition(AllocateLocalPartition message){
        int partitionId = message.getPartition().getPartitionId();
        if (localPartitions[partitionId] == null) {
            localPartitions[partitionId] = getContext().actorOf(PartitionActor.props(), "partition"+partitionId);
            getContext().watch(localPartitions[partitionId]);
            //todo gestirlo
        }
        localPartitions[partitionId].forward(message, getContext());

        sender().tell(new AllocationCompleted(),self());
    }


    //non fa niente
    @Override
    public void preStart() {
        System.out.println("I started "  + getContext().getSelf().path());
    }

    //non fa niente
    @Override
    public void postStop(){
        cluster.leave(cluster.selfMember().address());
        System.out.println("Sono morto: " + getContext().getSelf().path());

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