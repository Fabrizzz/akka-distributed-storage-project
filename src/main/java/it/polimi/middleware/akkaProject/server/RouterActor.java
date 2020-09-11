package it.polimi.middleware.akkaProject.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.actor.Props;
import akka.pattern.Patterns;
import it.polimi.middleware.akkaProject.dataStructures.Partition;
import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingActorRefs;
import it.polimi.middleware.akkaProject.messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;


import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;


public class RouterActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    List<PartitionRoutingActorRefs> partitionRoutingActorRefs;

    public static Props props() {
        return Props.create(RouterActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RoutingActorRefInitialConfiguration.class, this::initialConfiguration)
                .match(GetData.class, this::getData)
                .match(PutNewData.class, this::putNewData)
                .match(RoutingActorRefUpdate.class, this::update)
                .matchAny(o -> log.error("received unknown message"))
                .build();
    }

    public void update(RoutingActorRefUpdate message){
        partitionRoutingActorRefs.remove(message.getPartitionId());
        partitionRoutingActorRefs.add(message.getPartitionId(),message.getList());
        log.info("Routing Infos updated about partition: " + message.getPartitionId());
    }

    public void initialConfiguration(RoutingActorRefInitialConfiguration message){
        partitionRoutingActorRefs = message.getList();
    }


    public void getData(GetData message){
        int partitionId = message.getKey().hashCode() % partitionRoutingActorRefs.size();
        Collections.shuffle(partitionRoutingActorRefs.get(partitionId).getReplicas());
        for (ActorRef replica : partitionRoutingActorRefs.get(partitionId).getReplicas()) {
            Future<Object> secondReply = Patterns.ask(replica, message, 1000);
            try {
                Object reply = Await.result(secondReply, Duration.Inf());
                if (reply instanceof DataReply) {
                    sender().forward(reply, getContext());
                    return;
                }
            } catch (Exception e) {
                log.warning("Couldn't receive any get answer in time from " + replica.path(), e);
            }
        }
        sender().forward(new DataNotFound(), getContext());
        log.warning("Nobody answered to the Get Request");

    }

    public void putNewData(PutNewData message) {
        int partitionId = message.getKey().hashCode() % partitionRoutingActorRefs.size();
        ActorRef leader = partitionRoutingActorRefs.get(partitionId).getLeader();
        if (leader != null) {
            Future<Object> reply = Patterns.ask(leader, message, 5000);
            try {
                sender().forward(Await.result(reply, Duration.Inf()), getContext());
            } catch (Exception e) {
                log.warning("Couldn't receive any put confirmation in time", e);
            }
        }
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