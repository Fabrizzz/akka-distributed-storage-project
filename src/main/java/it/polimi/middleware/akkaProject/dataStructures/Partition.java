package it.polimi.middleware.akkaProject.dataStructures;

import java.io.Serializable;
import java.util.HashMap;

/** this class stores Datas and state of a Partition*/
public class Partition implements Serializable{
    private final int partitionId;
    private int state = 0;
    private final HashMap<Serializable, DataWithTimestamp> map;


    public Partition(int partitionId) {
        this.partitionId = partitionId;
        map = new HashMap<>();
    }

    public Partition(int partitionId, int state, HashMap<Serializable, DataWithTimestamp> map) {
        this.partitionId = partitionId;
        this.state = state;
        this.map = map;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public int getState() {
        return state;
    }

    public HashMap<Serializable, DataWithTimestamp> getMap() {
        return map;
    }

    public void incrementState(){
        state++;
    }

    public Partition getPartitionCopy(){
        return new Partition(partitionId, state, new HashMap<>(map));
    }
}

