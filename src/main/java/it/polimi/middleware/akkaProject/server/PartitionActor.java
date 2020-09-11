package it.polimi.middleware.akkaProject.server;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.pattern.Patterns;
import akka.util.Timeout;
import it.polimi.middleware.akkaProject.dataStructures.Partition;
import it.polimi.middleware.akkaProject.dataStructures.DataWithTimestamp;
import it.polimi.middleware.akkaProject.messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class PartitionActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private Partition partition;
    private boolean iAmLeader = false;
    private List<ActorRef> otherReplicas; //useful only if i am the leader


    public static Props props() {
        return Props.create(PartitionActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AllocateLocalPartition.class, this::allocateLocalPartition)
                .match(BecomeLeader.class, this::becomeLeader)
                .match(PutNewData.class, this::putData)
                .match(SnapshotReplicaRequest.class, m -> snapshotReplicaRequest())
                .match(GetPutUpdate.class, this::getPutUpdate)
                .match(GetData.class, this::getData)
                .matchAny(o -> log.error("received unknown message"))
                .build();
    }

    private void snapshotReplicaRequest(){
        iAmLeader = false;
        otherReplicas = null;
        sender().tell(new SnapshotReplica(partition.getPartitionCopy()), self());
    }

    //Get request by a Router
    private void getData(GetData message){
        log.info("Just received a get Request");
        boolean sentReply = false;
        if (partition.getMap().containsKey(message.getKey())) {
            DataWithTimestamp data = partition.getMap().get(message.getKey());
            if (data.getGeneratedAt().compareTo(message.getGeneratedAtLeastAt()) >= 0) {
                sender().tell(new DataReply(partition.getMap().get(message.getKey())), self());
                log.info("Just sent the get Reply");
                sentReply = true;
            }
        }
        if (!sentReply) {
            sender().tell(new DataNotFound(), self());
            log.warning("Couldn't find data request");
        }
    }

    //PutUpdate received from the leader
    private void getPutUpdate(GetPutUpdate message){
        if (partition.getState() +1 == message.getState()){
            partition.getMap().put(message.getMessage().getKey(), message.getMessage().getData());
            partition.incrementState();
            sender().tell(new PutUpdateReceived(), self());
            log.info("Just accepted a put Update");
        }
        else
            log.warning("Couldn't elaborate put Update because wrong partition state");

    }

    //snapshot received by the leader (cause i wasn't able to answer to the update in time)
    /*private void snapshotReplica(SnapshotReplica message){
        log.info("Just received a snapshot replica cause i didnt answer to putUpdate in time");
        if (partition.getState() < message.getReplica().getState())
            partition = message.getReplica();

    }*/

    //become leader request from the master, retrieving all the replica's actorRef
    private void becomeLeader(BecomeLeader message){
        log.info("Master just made me the leader of Partition");
        iAmLeader = true;
        otherReplicas = new ArrayList<>();
        for (Address otherReplica : message.getOtherReplicas()) {
            Future<ActorRef> reply = getContext().actorSelection(otherReplica + "/user/supervisor/partition" + partition.getPartitionId()).resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
            try {
                otherReplicas.add(Await.result(reply, scala.concurrent.duration.Duration.Inf()));
            } catch (Exception e) {
                log.error("Wasn't able to retrieve actorRef of " + otherReplica);
                sender().tell(new UnableToContactReplicas(), self());
                iAmLeader = false;
                return;
            }
        }
        sender().tell(new IAmLeader(), self());
        log.info("Just finished to retrieve actorRefs of other Replicas");
    }

    //Put Request received by a router
    private void putData(PutNewData message) throws InterruptedException {
        if (!iAmLeader){
            log.info("Just refused a Put Request cause i am not the leader");
            sender().tell(new NotALeader(), self());
        }
        else{
            if (partition.getMap().containsKey(message.getKey()) && partition.getMap().get(message.getKey()).getGeneratedAt().compareTo(message.getData().getGeneratedAt()) >= 0) {
                sender().tell(new DataIsTooOld(), self());
                log.info("Just refused a Put Request cause it is too old");
            }
            else {
                log.info("Just Accepted a Put Request");
                partition.getMap().put(message.getKey(), message.getData());
                partition.incrementState();
                System.out.println("I am the leader, i accepted a Put and my state is now: " + partition.getState());
                for (ActorRef otherReplica : otherReplicas) {
                    Future<Object> reply = Patterns.ask(otherReplica, new GetPutUpdate(message, partition.getState()), 1000);
                    try {
                        Await.result(reply, Duration.Inf());
                    } catch (TimeoutException e) {
                        //just in case he wasn't able to answer in time, at least he will eventually receive the update
                        //otherReplica.tell(new SnapshotReplica(this.partition), self());
                        //todo meglio implementarsi a mano una stash su ogni partition
                        //todo stasho richieste di snapshot dal master finchè non ricevo le put??? risky but it should work
                    }
                }
                sender().tell(new PutCompleted(), getSelf());
                log.info("Just completed a Put Request");
            }
        }
    }

    //allocate partition received by the master (either cause it's a new partition, or the leader just changed)
    private void allocateLocalPartition(AllocateLocalPartition message){
        log.info("Just received a new Partition from the master");
        partition = message.getPartition();
        iAmLeader = false;
        otherReplicas = null;
    }

    //non fa niente
    @Override
    public void preStart() {
        System.out.println("I started "  + getContext().getSelf().path());
    }

    //non fa niente
    @Override
    public void postStop(){
        System.out.println("I just died: " + getContext().getSelf().path());

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