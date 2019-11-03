package com.zman.scuttlebutt;


import java.util.function.Consumer;

public interface IOHandler {

    void send(byte[] bytes);

    void onListen(Consumer<byte[]> consumer);
}
