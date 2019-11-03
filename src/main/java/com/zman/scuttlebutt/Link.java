package com.zman.scuttlebutt;

public class Link {

    public static void link(Stream a, Stream b){
        a.link(b);
        b.link(a);
    }

}
