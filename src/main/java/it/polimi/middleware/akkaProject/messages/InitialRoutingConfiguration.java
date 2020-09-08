package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.PartitionRoutingInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InitialRoutingConfiguration implements Serializable {
    private final List<PartitionRoutingInfo> partitionRoutingInfos;


    public InitialRoutingConfiguration(List<PartitionRoutingInfo> partitionRoutingInfos) {
        this.partitionRoutingInfos = partitionRoutingInfos;
    }

    public List<PartitionRoutingInfo> getPartitionRoutingInfos() {
        return new ArrayList<>(partitionRoutingInfos);
    }
}
