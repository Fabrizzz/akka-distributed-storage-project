package it.polimi.middleware.akkaProject.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.actor.Props;
import akka.util.Timeout;
import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingInfo;
import it.polimi.middleware.akkaProject.dataStructures.RoutingActorRefs;
import it.polimi.middleware.akkaProject.messages.GetData;
import it.polimi.middleware.akkaProject.messages.InitialRoutingConfiguration;
import it.polimi.middleware.akkaProject.messages.PutNewData;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


public class RouterActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    Cluster cluster = Cluster.get(getContext().system());
    List<RoutingActorRefs> partitionInfos;

    public static Props props() {
        return Props.create(RouterActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InitialRoutingConfiguration.class, this::initialConfiguration)
                .match(GetData.class, this::getData)
                .match(PutNewData.class, this::putNewData)
                .matchAny(o -> log.error("received unknown message"))
                .build();
    }

    public void getData(GetData message){

    }

    public void putNewData(PutNewData message){


    }

    public void initialConfiguration(InitialRoutingConfiguration message){
        partitionInfos = new ArrayList<>();
        for (PartitionRoutingInfo partitionRoutingInfo : message.getPartitionRoutingInfos()) {
            RoutingActorRefs currentPartition = new RoutingActorRefs(partitionRoutingInfo.getPartitionId());
            Future<ActorRef> reply = getContext().actorSelection(partitionRoutingInfo.getLeader() + "/user/supervisor/partition"+partitionRoutingInfo.getPartitionId()).resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
            try {
                currentPartition.setLeader( Await.result(reply, Duration.Inf()));
            } catch (Exception e) {
                log.error("Couldn't contact a leader");
            }
            for (Address replica : partitionRoutingInfo.getReplicas()) {
                reply = getContext().actorSelection(replica + "/user/supervisor/partition"+partitionRoutingInfo.getPartitionId()).resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
                try {
                    currentPartition.getReplicas().add( Await.result(reply, Duration.Inf()));
                } catch (Exception e) {
                    log.error("Couldn't contact a replica");
                }
            }
            //todo
            //partitionInfos.add();
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