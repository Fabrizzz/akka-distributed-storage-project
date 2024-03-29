package it.polimi.middleware.akkaProject.server;

import akka.actor.*;
import akka.cluster.Member;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import akka.util.Timeout;
import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingActorRefs;
import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingMembers;
import it.polimi.middleware.akkaProject.messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


public class RouterManagerActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final int numberOfRouters;
    public int currentRouter = 0;
    private final List<ActorRef> routers;

    public RouterManagerActor(int numberOfRouters) {
        this.numberOfRouters = numberOfRouters;
        routers = new ArrayList<>(numberOfRouters);
    }

    public static Props props(int maxNumWorkers) {
        return Props.create(RouterManagerActor.class, maxNumWorkers);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(//
                1, //
                Duration.create("10 seconds"), //
                DeciderBuilder //
                        .match(Exception.class, ex -> SupervisorStrategy.resume()) //
                        .build());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RoutingConfigurationMessage.class, this::initialConfiguration)
                .match(RoutingConfigurationUpdate.class, this::update)
                .match(GetData.class, this::getData)
                .match(PutNewData.class, this::putNewData)
                .matchAny(o -> log.error("received unknown message"))
                .build();
    }


    public void getData(GetData message) {
        routers.get(currentRouter++ % routers.size()).forward(message, getContext());
    }

    public void putNewData(PutNewData message) {
        routers.get(currentRouter++ % routers.size()).forward(message, getContext());
    }

    public void update(RoutingConfigurationUpdate message) {
        int partitionId = message.getPartitionId();
        PartitionRoutingActorRefs currentPartitionRoutingActorRefs = new PartitionRoutingActorRefs(partitionId);
        Future<ActorRef> reply = getContext().actorSelection(message.getPartitionRoutingMembers().getLeader().address() + "/user/supervisor/partition" + partitionId).resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
        try {
            currentPartitionRoutingActorRefs.setLeader(Await.result(reply, Duration.Inf()));
            currentPartitionRoutingActorRefs.getReplicas().add(currentPartitionRoutingActorRefs.getLeader());
        } catch (Exception e) {
            log.warning("Couldn't contact the leader of replica: " + message.getPartitionId(), e);
        }
        for (Member member : message.getPartitionRoutingMembers().getReplicas()) {
            if (!member.equals(message.getPartitionRoutingMembers().getLeader())) {
                reply = getContext().actorSelection(member.address() + "/user/supervisor/partition" + partitionId).resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
                try {
                    currentPartitionRoutingActorRefs.getReplicas().add(Await.result(reply, Duration.Inf()));
                } catch (Exception e) {
                    log.warning("Couldn't contact replica: " + member.address() + " of partition" + partitionId, e);
                }
            }
        }
        RoutingActorRefUpdate update = new RoutingActorRefUpdate(partitionId, currentPartitionRoutingActorRefs);
        for (ActorRef router : routers) {
            router.tell(update, self());
        }
    }

    public void initialConfiguration(RoutingConfigurationMessage message) {
        ArrayList<PartitionRoutingActorRefs> newPartitionRoutingActorRefs = new ArrayList<>();
        for (PartitionRoutingMembers currentPartitionRoutingMembers : message.getPartitionRoutingInfos()) {
            int partitionId = currentPartitionRoutingMembers.getPartitionId();
            PartitionRoutingActorRefs currentPartitionRoutingActorRefs = new PartitionRoutingActorRefs(partitionId);
            Future<ActorRef> reply = getContext().actorSelection(currentPartitionRoutingMembers.getLeader().address() + "/user/supervisor/partition" + partitionId).resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
            try {
                currentPartitionRoutingActorRefs.setLeader(Await.result(reply, Duration.Inf()));
                currentPartitionRoutingActorRefs.getReplicas().add(currentPartitionRoutingActorRefs.getLeader());
            } catch (Exception e) {
                log.error("Couldn't contact the leader of replica: " + currentPartitionRoutingMembers.getPartitionId());
            }
            for (Member currentPartitionRoutingMember : currentPartitionRoutingMembers.getReplicas()) {
                if (!currentPartitionRoutingMember.equals(currentPartitionRoutingMembers.getLeader())) {
                    reply = getContext().actorSelection(currentPartitionRoutingMember.address() + "/user/supervisor/partition" + partitionId).resolveOne(new Timeout(scala.concurrent.duration.Duration.create(1, TimeUnit.SECONDS)));
                    try {
                        currentPartitionRoutingActorRefs.getReplicas().add(Await.result(reply, Duration.Inf()));
                    } catch (Exception e) {
                        log.error("Couldn't contact a replica");
                    }
                }
            }
            newPartitionRoutingActorRefs.add(currentPartitionRoutingActorRefs);
        }

        log.info("Finished obtaining all the Replicas ActorRef");


        for (ActorRef router : routers) {
            router.tell(new RoutingActorRefInitialConfiguration(newPartitionRoutingActorRefs), self());
        }
    }

    //non fa niente
    @Override
    public void preStart() {
        System.out.println("I started " + getContext().getSelf().path());
        for (int i = 0; i < numberOfRouters; i++) {
            routers.add(getContext().actorOf(RouterActor.props()));
        }
        log.info("I started " + numberOfRouters + " routers");
    }

    //non fa niente
    @Override
    public void postStop() {
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
        super.preRestart(reason, message);
    }

    //chiama preStart
    @Override
    public void postRestart(Throwable reason) throws Exception {
        System.out.println("I restarted " + getContext().getSelf().path());
        super.postRestart(reason);
    }
}
