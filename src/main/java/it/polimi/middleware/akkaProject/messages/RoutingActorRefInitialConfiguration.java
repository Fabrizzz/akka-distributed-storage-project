package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingActorRefs;

import java.io.Serializable;
import java.util.ArrayList;

public class RoutingActorRefInitialConfiguration implements Serializable {

    private final ArrayList<PartitionRoutingActorRefs> list;


    public RoutingActorRefInitialConfiguration(ArrayList<PartitionRoutingActorRefs> list) {
        this.list = list;
    }

    public ArrayList<PartitionRoutingActorRefs> getList() {
        return new ArrayList<>(list);
    }
}
