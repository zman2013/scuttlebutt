package com.zman.scuttlebutt;

import com.zman.pull.stream.IDuplex;
import com.zman.scuttlebutt.bean.StreamOptions;
import com.zman.scuttlebutt.bean.Update;
import example.AsyncModel;
import example.Model;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static com.zman.pull.stream.util.Pull.link;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AsyncScuttlebuttTest {

    /**
     * 验证握手协议
     */
    @Test
    public void shakehands(){
        AsyncScuttlebutt a = new AsyncModel("a");
        AsyncScuttlebutt b = new AsyncModel("b");

        Consumer aIncoming = mock(Consumer.class);
        Consumer aSynced = mock(Consumer.class);
        Consumer aClosed = mock(Consumer.class);
        a.on("incoming", aIncoming);
        a.on("synced", aSynced);
        a.on("close", aClosed);

        Consumer bIncoming = mock(Consumer.class);
        Consumer bSynced = mock(Consumer.class);
        Consumer bClosed = mock(Consumer.class);
        b.on("incoming", bIncoming);
        b.on("synced", bSynced);
        b.on("close", bClosed);

        IDuplex aDuplex = a.createSbStream();
        IDuplex bDuplex = b.createSbStream();

        link(aDuplex, bDuplex);

        aDuplex.close();
        bDuplex.close();

        verify(aIncoming, times(1)).accept(any());
        verify(aSynced, times(1)).accept("b");
        verify(aClosed, times(1)).accept("b");
        verify(bIncoming, times(1)).accept(any());
        verify(bSynced, times(1)).accept("a");
        verify(bClosed, times(1)).accept("a");
    }

    /**
     * 各自跟新一条数据
     */
    @Test
    public void bothABUpdate() throws ExecutionException, InterruptedException {
        AsyncModel a = new AsyncModel("a");
        AsyncModel b = new AsyncModel("b");

        Consumer aIncoming = mock(Consumer.class);
        Consumer aSynced = mock(Consumer.class);
        Consumer aClosed = mock(Consumer.class);
        Consumer aUpdate = mock(Consumer.class);
        a.on("incoming", aIncoming);
        a.on("synced", aSynced);
        a.on("close", aClosed);
        a.on("_update", aUpdate);

        Consumer bIncoming = mock(Consumer.class);
        Consumer bSynced = mock(Consumer.class);
        Consumer bClosed = mock(Consumer.class);
        Consumer bUpdate = mock(Consumer.class);
        b.on("incoming", bIncoming);
        b.on("synced", bSynced);
        b.on("close", bClosed);
        b.on("_update", bUpdate);

        IDuplex aDuplex = a.createSbStream();
        IDuplex bDuplex = b.createSbStream();

        link(aDuplex, bDuplex);

        CompletableFuture af = a.set("a-key", 1);
        CompletableFuture bf = b.set("b-key", 2);

        af.get();
        bf.get();

        aDuplex.close();
        bDuplex.close();

        verify(aIncoming, times(1)).accept(any());
        verify(aSynced, times(1)).accept("b");
        verify(aClosed, times(1)).accept("b");
        // 一次update来自自己，一次来自peer
        verify(aUpdate, times(2)).accept(any(Update.class));
        verify(bIncoming, times(1)).accept(any());
        verify(bSynced, times(1)).accept("a");
        verify(bClosed, times(1)).accept("a");
        // 一次update来自自己，一次来自peer
        verify(bUpdate, times(2)).accept(any(Update.class));

        Assert.assertNotEquals(1, b.get("a-key"));
        Assert.assertNotEquals(2, a.get("b-key"));
    }



}
