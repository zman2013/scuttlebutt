package com.zman.scuttlebutt.bean;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Comparator;
import java.util.Objects;

@ToString
@NoArgsConstructor
public class Update<T> implements Comparable<Update> {
    public T data;

    public long timestamp;

    public String sourceId;

    public String from;

    public Update(T data, long timestamp, String sourceId, String from){
        this.data = data;
        this.timestamp = timestamp;
        this.sourceId = sourceId;
        this.from = from;
    }

    @Override
    public Update<T> clone(){
        return new Update(data, timestamp, sourceId, from);
    }

    @Override
    public int compareTo(Update b) {
        return Long.compare(this.timestamp, b.timestamp);
    }
}