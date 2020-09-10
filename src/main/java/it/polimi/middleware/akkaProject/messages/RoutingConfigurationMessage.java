package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingMembers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoutingConfigurationMessage implements Serializable {
    private final List<PartitionRoutingMembers> partitionRoutingAddresses;


    public RoutingConfigurationMessage(List<PartitionRoutingMembers> partitionRoutingAddresses) {
        this.partitionRoutingAddresses = partitionRoutingAddresses;
    }

    public List<PartitionRoutingMembers> getPartitionRoutingInfos() {
        return new ArrayList<>(partitionRoutingAddresses);
    }
}
