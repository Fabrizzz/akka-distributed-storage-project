package it.polimi.middleware.akkaProject.client;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import it.polimi.middleware.akkaProject.dataStructures.SavedData;
import it.polimi.middleware.akkaProject.messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class ClientActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final String masterAddress;
    private HashMap<Serializable, SavedData> map = new HashMap<>();
    private ActorRef master;
    private ArrayList<ActorRef> routers = new ArrayList<>();
    Random random = new Random();

    public ClientActor(String masterAddress) {
        this.masterAddress = masterAddress;
    }

    public static Props props(String masterAddress) {
        return Props.create(ClientActor.class, masterAddress);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(UpdateRoutersList.class, this::updateRouters)
                .match(PutNewData.class, this::putNewData)
                .match(ForwardGetData.class, this::getData)
                .matchAny(o -> log.error("received unknown message"))
                .build();
    }

    private void updateRouters(UpdateRoutersList message){
        Future<Object> reply = Patterns.ask(master, message, 1000);
        ArrayList<Address> addresses = null;
        try {
            addresses = (ArrayList<Address>) Await.result(reply, Duration.Inf());
        } catch (Exception e) {
            System.out.println("Couldn't contact the master");
            return;
        }

        for (Address address : addresses) {
            Future<ActorRef> secondReply = getContext().actorSelection(address +  "/user/routerManager").resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
            try {
                routers.add(Await.result(secondReply, Duration.Inf()));
            } catch (Exception e) {
                System.out.println("Couldn't get a Router ref");
            }
        }

    }

    private void putNewData(PutNewData message){
        int router = random.nextInt(routers.size());
        Future<Object> reply = Patterns.ask(routers.get(router), message, 3000);
        try {
            Object secondReply = Await.result(reply, Duration.Inf());
            if (secondReply instanceof NotALeader)
                System.out.println("The router contacted someone who was not a leader");
            else if (secondReply instanceof DataIsTooOld)
                System.out.println("Timestamp was too old and ignored");
            else if (secondReply instanceof PutCompleted)
                System.out.println("Put Completed");
        } catch (Exception e) {
            System.out.println("Didn't receive any answer");
        }
    }

    private void getData(ForwardGetData message){
        if (!map.containsKey(message.getKey()))
            System.out.println("Cannot get something i have never put");
        else {
            int router = random.nextInt(routers.size());
            Future<Object> reply = Patterns.ask(routers.get(router), new GetData(message.getKey(), map.get(message.getKey()).getGeneratedAt()), 3000);
            try {
                DataReply secondReply = (DataReply) Await.result(reply, Duration.Inf());
                System.out.println("I got: " + secondReply.getData().getData());
            } catch (Exception e) {
                System.out.println("Didn't receive any answer");
            }
        }

    }

    //non fa niente
    @Override
    public void preStart() {
        System.out.println("I started "  + getContext().getSelf().path());
        Future<ActorRef> reply = getContext().actorSelection("akka.tcp://ClusterSystem@" + masterAddress + ":1234" + "/user/master").resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
        try {
            master = Await.result(reply, Duration.Inf());
        } catch (Exception e) {
            log.error("Couldn't contact the Master");
            getContext().system().terminate();
        }
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