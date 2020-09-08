package it.polimi.middleware.akkaProject.messages;

import it.polimi.middleware.akkaProject.dataStructures.Partition;

import java.io.Serializable;

public class AllocateLocalPartition implements Serializable {
    private final Partition partition;

    public AllocateLocalPartition(Partition partition) {
        this.partition = partition;
    }

    public Partition getPartition() {
        return partition.getPartitionCopy();
    }

}
