package it.polimi.middleware.akkaProject.dataStructures;

import java.io.Serializable;
import java.util.HashMap;

public class Partition implements Serializable{
    private final int partitionId;
    private int state = 0;
    private HashMap<Serializable, SavedData> map;


    public Partition(int partitionId) {
        this.partitionId = partitionId;
        map = new HashMap<>();
    }

    public Partition(int partitionId, HashMap<Serializable, SavedData> map) {
        this.partitionId = partitionId;
        this.map = map;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void incrementState(){
        state++;
    }

    public HashMap<Serializable, SavedData> getMap() {
        return map;
    }

    public Partition getPartitionCopy(){
        return new Partition(partitionId, new HashMap<>(map));
    }
}

