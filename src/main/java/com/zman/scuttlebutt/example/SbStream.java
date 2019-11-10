package com.zman.scuttlebutt.example;

import com.zman.scuttlebutt.example.Scuttlebutt.Update;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class SbStream {

    public Scuttlebutt sb;

    public Map<String, Long> peerSources = new HashMap<>();
    public String peerId = "";

    public Duplex duplex;


    public SbStream(Scuttlebutt scuttlebutt) {
        this.sb = scuttlebutt;

        sb.streams++;

        duplex = new Duplex(sb.id, this::onData, null);

        Outgoing outgoing = new Outgoing(sb.id, sb.sources);

        duplex.push(outgoing);

    }


    boolean syncRecv = false;
    public boolean onData(Object update){
        if( update instanceof Update){
            return sb.applyUpdate((Update) update);
        }else if( update instanceof  String){
            String cmd = (String) update;
            if( "SYNC".equals(cmd)){
                log.debug("{} SYNC received", sb.id);
                syncRecv = true;
            }
        }else{
            start((Outgoing) update);
        }
        return true;
    };

    public void onClose(){
        sb.removeListener("_update", onUpdate);
        sb.streams--;
    }

    public void start(Outgoing incoming){
        log.info("{} start with incoming data: {}", sb.id, incoming);

        peerSources = incoming.sources; // deepCopy
        peerId = incoming.id;

        if(!duplex.readable){
            promiseResolved(new Update[0]);
            return;
        }

        if( AsyncScuttlebutt.class.isAssignableFrom(sb.getClass())){
            AsyncScuttlebutt asyncSb = (AsyncScuttlebutt) sb;
            asyncSb.reentrantLock(() -> {       // 读取history，和监听sb上的update必须是原子性的，否则可能漏消息
                Update[] history = sb.history(peerSources);
                promiseResolved(history);
            });
        }else{
            Update[] history = sb.history(peerSources);
            promiseResolved(history);
        }

    }

    boolean syncSent = false;
    private void promiseResolved(Update[] history) {
        Arrays.stream(history)
                .forEach(update -> {
                    update.from = sb.id;
                    duplex.push(update);
                });

        log.info("{} sent history to peer: {}", sb.id, history);

        sb.on("_update", onUpdate);

        duplex.push("SYNC");

        syncSent = true;

        log.info("{} sent SYNC to peer", sb.id);
    }

    public Consumer<Update> onUpdate = this::onUpdate;  // 单独声明Consumer属性
    public Update onUpdate(Update update){
        log.info("got update on stream: {}", update);

        // 不可读
        if( !duplex.readable ){
            log.info("update ignore by it's non-readable flag");
            return update;
        }

        // 过滤：对端时间戳更新
        if( peerSources.get(update.sourceId) > update.timestamp ){
            return update;
        }
        // 过滤：更新来自对端
        if( update.from.equals(peerId)){
            return update;
        }

        // 发送到对端
        update.from = sb.id;
        duplex.push(update);
        log.info("sent update to peer: {}", update);

        // 更新本地保存的对端的时间戳
        peerSources.put(update.sourceId, update.timestamp);

        return update;
    };


    @ToString
    private class Outgoing {
        String id;
        Map<String, Long> sources;
        Object meta;
        Object accept;

        public Outgoing(String id, Map<String, Long> sources) {
            this.id = id;
            this.sources = sources; // should be deepCopy
        }
    }
}
