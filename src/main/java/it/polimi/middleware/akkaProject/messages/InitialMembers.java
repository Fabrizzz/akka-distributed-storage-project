package it.polimi.middleware.akkaProject.messages;

import akka.cluster.Member;

import java.util.ArrayList;

public class InitialMembers {
    private final ArrayList<Member> initialMembers;

    public InitialMembers(ArrayList<Member> initialMembers) {
        this.initialMembers = initialMembers;
    }

    public ArrayList<Member> getInitialMembers() {
        return initialMembers;
    }
}
