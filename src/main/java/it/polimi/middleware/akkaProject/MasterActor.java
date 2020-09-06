package it.polimi.middleware.akkaProject;

import akka.actor.AbstractActor;
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

public class MasterActor extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Cluster cluster = Cluster.get(getContext().system());


    @Override
    public void preStart() {
        cluster.registerOnMemberUp(
                () -> cluster.subscribe(getSelf(), MemberEvent.class, UnreachableMember.class));

    }

    // Re-subscribe when restart
    @Override
    public void postStop() {
        cluster.unsubscribe(self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()//
                .match(MemberUp.class, this::onMemberUp) //
                .match(UnreachableMember.class, this::onUnreachableMember) //
                .match(MemberRemoved.class, this::onMemberRemoved) //
                .match(ClusterEvent.CurrentClusterState.class, this::onClusterState) //
                .match(MemberEvent.class, this::onMemberEvent) //
                .build();
    }

    private void onMemberUp(MemberUp msg) {
        log.info("Member is Up: {}", msg.member());
		/*if (!msg.member().equals( cluster.selfMember()) )
			cluster.down(msg.member().address());*/
    }

    private void onUnreachableMember(UnreachableMember msg) {
        System.out.println("XXXXXXXXXXXXXXXXXXXXX");
        System.out.println(msg.member().uniqueAddress());
        log.info("Member detected as unreachable: {}", msg.member());
        cluster.down(msg.member().address());
    }

    private void onMemberRemoved(MemberRemoved msg) {
        log.info("Member is Removed: {}", msg.member());
    }

    private void onClusterState(ClusterEvent.CurrentClusterState msg) {
        for (final Member member : msg.getMembers()) {
            if (member.status().equals(MemberStatus.up())) {
                if (member.hasRole("master")) {
                    getContext().actorSelection(member.address() + "/user/master").tell("ciao", self());
                }
            }
        }
    }

    private void onMemberEvent(MemberEvent msg) {
        System.out.println("I just ignored " + msg.getClass().getCanonicalName());
    }

    public static Props props() {
        return Props.create(MasterActor.class);
    }
}