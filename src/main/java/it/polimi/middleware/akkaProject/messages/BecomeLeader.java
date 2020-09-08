package it.polimi.middleware.akkaProject.messages;

import akka.actor.Address;

import java.io.Serializable;
import java.util.ArrayList;

public class BecomeLeader implements Serializable {
    private final ArrayList<Address> otherReplicas;


    public BecomeLeader(ArrayList<Address> otherReplicas) {
        this.otherReplicas = otherReplicas;
    }

    public ArrayList<Address> getOtherReplicas() {
        return otherReplicas;
    }
}
