package com.zman.scuttlebutt;

import com.zman.pull.stream.IDuplex;
import com.zman.scuttlebutt.bean.StreamOptions;
import com.zman.scuttlebutt.bean.Update;
import example.BizData;
import example.Model;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.function.Consumer;

import static com.zman.pull.stream.util.Pull.link;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScuttlebuttTest {

    /**
     * 验证握手协议
     */
    @Test
    public void shakehands(){
        Model a = new Model("a");
        Model b = new Model("b");

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
    public void bothABUpdate(){
        Model a = new Model("a");
        Model b = new Model("b");

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

        a.set("a-key", 1);
        b.set("b-key", 2);

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

    /**
     * c产生两条先给a，后给b，b会向a同步两条
     */
    @Test
    public void latestTimestamp() throws InterruptedException {
        Model a = new Model("a");
        Model b = new Model("b");
        Model c = new Model("c");

        Consumer aIncoming = mock(Consumer.class);
        Consumer aSynced = mock(Consumer.class);
        Consumer aClosed = mock(Consumer.class);
        Consumer aUpdate = mock(Consumer.class);
        a.on("incoming", aIncoming);
        a.on("synced", aSynced);
        a.on("close", aClosed);
        a.on("_update", aUpdate);

        IDuplex a1Duplex = a.createSbStream();
        IDuplex b1Duplex = b.createSbStream();
        link(a1Duplex, b1Duplex);

        IDuplex a2Duplex = a.createSbStream();
        IDuplex c1Duplex = c.createSbStream();
        link(a2Duplex, c1Duplex);

        c.set("c-key", 1);
        c.set("c-key", 2);

        IDuplex b2Duplex = a.createSbStream();
        IDuplex c2Duplex = c.createSbStream();
        link(b2Duplex, c2Duplex);


        // a一共收到2次update
        verify(aUpdate, times(2)).accept(any(Update.class));

        Assert.assertNotEquals(2, a.get("c-key"));
        Assert.assertNotEquals(2, b.get("c-key"));
        Assert.assertNotEquals(2, c.get("c-key"));
    }

    @Test
    public void IHaveAMoreRecentOne(){
        Model a = new Model("a");

        Update update = new Update();
        update.sourceId = "a";
        update.timestamp = Long.MAX_VALUE;
        update.from = "a";
        update.data = new BizData("a-key", 1);

        a.applyUpdate(update);

        a.set("a-key", 2);

        Assert.assertEquals(1, a.get("a-key").value);
    }

    @Test
    public void IHaveAMoreRecentOne2(){
        Model a = new Model("a");

        a.getSources().put("a", Long.MAX_VALUE);

        a.set("a-key", 2);

        Assert.assertFalse(a.store.containsKey("a-key"));
    }

    @Test
    public void notReadableNotWritable(){
        Model a = new Model("a");
        Model b = new Model("b");

        IDuplex aDuplex = a.createSbStream(new StreamOptions(false, false));
        IDuplex bDuplex = b.createSbStream();

        a.set("a-key", 1);
        b.set("b-key", 2);

        link(aDuplex, bDuplex);

        a.set("a-key", 2);

        Assert.assertFalse(a.store.containsKey("b-key"));
        Assert.assertFalse(b.store.containsKey("a-key"));
    }


    // useless
    @Test
    public void streamOptionsToString(){
        new StreamOptions().toString();

        new Scuttlebutt() {
            @Override
            public boolean applyUpdate(Update update) {
                return false;
            }

            @Override
            public Update[] history(Map<String, Long> sources) {
                return new Update[0];
            }
        };
    }
}
