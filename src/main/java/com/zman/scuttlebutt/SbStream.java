package com.zman.scuttlebutt;

import com.zman.pull.stream.IDuplex;
import com.zman.pull.stream.impl.DefaultDuplex;
import com.zman.scuttlebutt.bean.StreamOptions;
import com.zman.scuttlebutt.bean.Update;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class SbStream{

    private Scuttlebutt sb;

    /**
     * 对端掌握的向量钟
     */
    private Map<String, Long> peerSources = new HashMap<>();
    private String peerId = "";

    /**
     * pull-stream双工流，负责pull-stream流上的相关功能
     * 从SbStream中剥离出去，让SbStream更简洁：
     *  只负责监控来自sb、duplex上的事件，不参与流的实际操作
     */
    @Getter
    private IDuplex duplex;

    private StreamOptions streamOptions;


    public SbStream(Scuttlebutt scuttlebutt, StreamOptions streamOptions) {
        this.sb = scuttlebutt;

        sb.streams++;

        this.streamOptions = streamOptions;

        duplex = new DefaultDuplex<>(this::onData, this::onClose);

        Outgoing outgoing = new Outgoing(sb.id, sb.getSources());

        duplex.push(outgoing);
    }

    /////////////   duplex listeners     ///////////////
    private boolean syncRecv = false;
    private boolean onData(Object update){
        if( update instanceof Outgoing){            // 收到对端的向量钟
            sb.emit("incoming", update);
            start((Outgoing) update);
        }else if( update instanceof  String){       // 收到指令
            sb.emit("cmd", update);
            String cmd = (String) update;
            processCommand(cmd);
        }else if( update instanceof Update){        // 收到对端的Update
            sb.emit("update", update);
            processUpdate((Update)update);
        }else{
            sb.emit("illegalScuttlebutt", update);
        }
        return false;
    }


    private boolean closed;
    private void onClose(Throwable throwable){
        sb.off("_update", onUpdate);
        sb.streams--;
        if(!closed) {
            closed = true;
            sb.emit("close", peerId);
        }
    }
    /////////////   duplex listeners end  ///////////////


    /////////////   sb actions      ///////////////
    private void start(Outgoing incoming){
        log.info("{} start with incoming data: {}", sb.id, incoming);

        peerSources = incoming.sources; // deepCopy
        peerId = incoming.id;

        if(!streamOptions.isReadable()){
            shakeHands(new Update[0]);
            return;
        }

        if( AsyncScuttlebutt.class.isAssignableFrom(sb.getClass())){
            AsyncScuttlebutt asyncSb = (AsyncScuttlebutt) sb;
            asyncSb.reentrantLock(() -> {       // 读取history，和监听sb上的update必须是原子性的，否则可能漏消息
                Update[] history = sb.history(peerSources);
                shakeHands(history);
            });
        }else{
            Update[] history = sb.history(peerSources);
            shakeHands(history);
        }

    }

    /**
     * 发出history（如果有）、SYNC，完成与对端的握手
     */
    private boolean syncSent = false;
    private void shakeHands(Update[] history) {
        Arrays.stream(history)
                .forEach(update -> {
                    update.from = sb.id;
                    duplex.push(update);
                });

        log.info("{} sent history to peer[{}]: {}", sb.id, peerId, history);

        sb.on("_update", onUpdate);

        duplex.push("SYNC");

        syncSent = true;

        log.info("{} sent SYNC to peer[{}]", sb.id, peerId);
    }

    /**
     * 处理stream上的指令
     * @param cmd 指令
     */
    private void processCommand(String cmd) {
        if( "SYNC".equals(cmd)){
            log.info("{} SYNC received", sb.id);
            syncRecv = true;
            if(syncSent){
                log.info("{} synced with peer {}", sb.id, peerId);
                sb.emit("synced", peerId);
            }
        }
    }

    /**
     * 处理stream上的update
     * @param update 更新
     */
    private void processUpdate(Update update) {
        if( streamOptions.isWritable() ) {
            sb.update(update);
        }else{
            log.debug("{} isn't writable, ignore update", sb.id);
        }
    }

    /**
     * 监听scuttlebutt上的更新
     */
    private Consumer<Update> onUpdate = this::onUpdate;  // 单独声明Consumer属性
    private void onUpdate(Update update){

        if(!syncSent) return;

        // 不可读
        if( !streamOptions.isReadable() ){
            log.debug("{} update ignore by it's non-readable flag", sb.id);
            return;
        }

        // 过滤：对端时间戳更新
        if( peerSources.computeIfAbsent(update.sourceId, k->0L) > update.timestamp ){
            log.debug("{} peer[{}]'s clock[{}] of sourceId[{}] is new, won't sent update",
                    sb.id, peerId, update.sourceId, peerSources.get(update.sourceId));
            return;
        }
        // 过滤：更新来自对端
        if( update.from.equals(peerId)){
            log.debug("{} update is from peer[{}], won't sent update", sb.id, peerId);
            return;
        }

        // 发送到对端
        update.from = sb.id;

        log.debug("{} push update to peer: {}", sb.id, update);
        duplex.push(update);

        // 更新本地保存的对端的时间戳，即：更新对端掌握的知识
        peerSources.put(update.sourceId, update.timestamp);
    }
    /////////////   sb actions end    ///////////////

    /**
     * 关闭stream
     */
    public void stop(){
        duplex.close();     // 关闭duplex，duplex会触发stream close
    }


    @ToString
    public class Outgoing {
        String id;
        Map<String, Long> sources;
        Object meta;
        Object accept;

        Outgoing(String id, Map<String, Long> sources) {
            this.id = id;
            this.sources = sources; // should be deepCopy
        }
    }

}
