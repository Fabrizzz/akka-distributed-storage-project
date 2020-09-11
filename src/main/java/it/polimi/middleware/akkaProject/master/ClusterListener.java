package it.polimi.middleware.akkaProject.master;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akkaProject.messages.ClusterChange;
import it.polimi.middleware.akkaProject.messages.InitialMembers;

import java.util.ArrayList;

/** This class Listen to Cluster events and communicate them to the master */
public class ClusterListener extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Cluster cluster = Cluster.get(getContext().system());
    ActorRef master;

    public ClusterListener(ActorRef master) {
        this.master = master;
    }

    public static Props props(ActorRef master) {
        return Props.create(ClusterListener.class, master);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()//
                .match(ClusterEvent.CurrentClusterState.class, this::onClusterState) //
                .matchAny(o -> log.error("received unexpected message before initial configuration"))
                .build();
    }

    private Receive defaultBehavior() {
        return receiveBuilder()//
                .match(MemberUp.class, this::onMemberUp) //
                .match(UnreachableMember.class, this::onUnreachableMember) //
                .match(MemberRemoved.class, this::onMemberRemoved) //
                .match(MemberEvent.class, this::onMemberEvent) //
                .build();
    }

    private void onMemberUp(MemberUp msg) {
        log.info("Member is Up: {}", msg.member());
        master.tell(new ClusterChange(), self());
    }

    private void onUnreachableMember(UnreachableMember msg) {

        log.info("Member detected as unreachable: {}", msg.member());
        cluster.down(msg.member().address());
        master.tell(new ClusterChange(), self());

    }

    private void onMemberRemoved(MemberRemoved msg) {
        log.info("Member is Removed: {}", msg.member());
        master.tell(new ClusterChange(), self());
    }

    private void onClusterState(ClusterEvent.CurrentClusterState msg) {
        ArrayList<Member> initialMembers = new ArrayList<>();
        for (final Member member : msg.getMembers()) {
            if (member.status().equals(MemberStatus.up()) && (member.hasRole("server"))) {
                initialMembers.add(member);
            }
        }
        master.tell(new InitialMembers(initialMembers), getSelf());
        getContext().become(defaultBehavior());
    }

    private void onMemberEvent(MemberEvent msg) {
        log.info("I just ignored " + msg.getClass().getCanonicalName());
    }

    @Override
    public void preStart() {
        cluster.registerOnMemberUp(
                () -> cluster.subscribe(getSelf(), MemberEvent.class, UnreachableMember.class));

    }


    @Override
    public void postStop() {
        cluster.state().getMembers().forEach(m -> {if (!m.equals(cluster.selfMember())) cluster.leave(m.address()
        );});
        getContext().system().terminate();

    }
}