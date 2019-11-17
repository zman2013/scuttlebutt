package com.zman.scuttlebutt.bean;

import lombok.*;

@Setter@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class StreamOptions {

    private boolean readable;

    private boolean writable;
}
