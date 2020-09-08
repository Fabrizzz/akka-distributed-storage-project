package it.polimi.middleware.akkaProject.server;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.actor.Props;
import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingInfo;
import it.polimi.middleware.akkaProject.messages.GetData;
import it.polimi.middleware.akkaProject.messages.InitialRoutingConfiguration;
import it.polimi.middleware.akkaProject.messages.PutNewData;


import java.util.List;
import java.util.Optional;


public class RouterActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    Cluster cluster = Cluster.get(getContext().system());
    List<PartitionRoutingInfo> partitionInfos;

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
        partitionInfos = message.getPartitionRoutingInfos();
        //todo convertire addresses in actorRef
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