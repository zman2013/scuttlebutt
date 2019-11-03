package com.zman.scuttlebutt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter@Setter
@AllArgsConstructor
@ToString
public class Update {

    private String attribute;

    private String value;

    private long timestamp;

}
