package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingAddresses;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoutingConfigurationMessage implements Serializable {
    private final List<PartitionRoutingAddresses> partitionRoutingAddresses;


    public RoutingConfigurationMessage(List<PartitionRoutingAddresses> partitionRoutingAddresses) {
        this.partitionRoutingAddresses = partitionRoutingAddresses;
    }

    public List<PartitionRoutingAddresses> getPartitionRoutingInfos() {
        return new ArrayList<>(partitionRoutingAddresses);
    }
}
