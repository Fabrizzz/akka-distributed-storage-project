package org.example;


import java.util.ArrayList;
import java.util.LinkedHashMap;

class PartitionedMap{
    private int numberOfPartitions;
    private int len;
    private ArrayList<LinkedHashMap<Integer, Integer>> maps;

    public PartitionedMap(int numberOfPartitions) {
        this.numberOfPartitions = numberOfPartitions;
        len = (int)((((long)Integer.MAX_VALUE)+1)/(numberOfPartitions /2));
        this.maps = new ArrayList<>(numberOfPartitions);
        for (int i = 0; i < numberOfPartitions; i++) {
            maps.add(new LinkedHashMap<>());
        }
    }

    public void add(int key, int val){
        int pos = getPos(key);
        maps.get(pos).put(key, val);
    }

    public int get(int key){
        int pos = getPos(key);
        return maps.get(pos).get(key);
    }

    private int getPos(int key){
        if (key >= 0){
            key /= len;
        }
        else{
            key = Math.abs((key + 1) / len) + numberOfPartitions / 2;
        }
        return key;
    }

    public void doublePartitions(){
        numberOfPartitions = numberOfPartitions *2;
        len = (int)((((long)Integer.MAX_VALUE)+1)/(numberOfPartitions /2));
        ArrayList<LinkedHashMap<Integer, Integer>> newMaps = new ArrayList<>(numberOfPartitions);
        for (LinkedHashMap<Integer, Integer> map : maps) {
            System.out.println(map.size());
        }
        for (int i = 0; i < numberOfPartitions; i++) {
            newMaps.add(new LinkedHashMap<>());
        }
        for (LinkedHashMap<Integer, Integer> map : maps) {
            map.forEach((key,val) ->{
                int pos = getPos(key);
                newMaps.get(pos).put(key,val);
            });
        }
        maps = newMaps;
        for (LinkedHashMap<Integer, Integer> map : maps) {
            System.out.println(map.size());
        }
    }

    public int getNumberOfPartitions() {
        return numberOfPartitions;
    }
}