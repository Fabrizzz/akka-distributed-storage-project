package it.polimi.middleware.akkaProject;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.actor.Props;


import java.util.Optional;


public class MyActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    Cluster cluster = Cluster.get(getContext().system());

    public static Props props() {
        return Props.create(MyActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("exception", m -> {throw new Exception();})
                .match(
                        String.class,
                        s -> log.info("Received String message: {}", s))
                .matchAny(o -> log.error("received unknown message"))
                .build();
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