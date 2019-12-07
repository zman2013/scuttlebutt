package com.zman.scuttlebutt.bean;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Update<T> {
    public T data;

    public long timestamp;

    public String sourceId;

    public String from;

}