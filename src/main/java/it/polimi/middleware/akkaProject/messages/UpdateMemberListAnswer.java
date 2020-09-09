package it.polimi.middleware.akkaProject.messages;

import akka.actor.Address;

import java.io.Serializable;
import java.util.ArrayList;

public class UpdateMemberListAnswer implements Serializable {
    private final ArrayList<Address> list;

    public UpdateMemberListAnswer(ArrayList<Address> list) {
        this.list = list;
    }

    public ArrayList<Address> getList() {
        return new ArrayList<>(list);
    }
}
