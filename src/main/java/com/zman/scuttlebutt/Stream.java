package com.zman.scuttlebutt;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@NoArgsConstructor
public class Stream {

    private Scuttlebutt sb;

    protected Map<NodeId, Stream> peerStreams = new ConcurrentHashMap<>();

    /**
     * 工作线程
     */
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private ScheduledExecutorService pullExector = Executors.newSingleThreadScheduledExecutor();


    public Stream(Scuttlebutt scuttlebutt) {
        this.sb = scuttlebutt;
    }

    /**
     * 连接另一端的stream
     * @param peerStream
     */
    public void link(Stream peerStream) {
        NodeId nodeId = peerStream.getNodeId();
        peerStreams.put(nodeId, peerStream);
        startListening();
    }

    public NodeId getNodeId() {
        return sb.getNodeId();
    }

    public void close() {
        broadcast(ScuttlebuttEvent.CLOSE, null);
    }

    /**
     * peer节点有状态更新，更新自己的model或状态
     *
     * @param payload
     */
    public void onData(NodeId peerNodeId, ScuttlebuttEvent event, Object payload) {
        switch (event) {
            case CLOSE:
                peerStreams.remove(peerNodeId);
                break;
            case UPDATE:
                if( payload != null ) {
                    sb.applyUpdates((Update[]) payload);
                }
                break;
            case REQUEST:
                Clock peerClock = (Clock) payload;

                executor.execute(()-> {
                    Update[] updates = sb.history(peerClock);
                    broadcast(ScuttlebuttEvent.UPDATE, updates);
                });
                break;
            default:
                log.error("invalid scuttlebutt event");
        }
    }

    /**
     * 简单实现将reactive-stream的request置为1
     */
    public void startListening(){
        pullExector.scheduleWithFixedDelay(
                ()->broadcast(ScuttlebuttEvent.REQUEST, sb.clock),
                0,
                1,
                TimeUnit.SECONDS
        );
    }

    /**
     * 向所有peer发送广播
     * @param event
     * @param payload
     */
    private void broadcast(ScuttlebuttEvent event, Object payload) {
        NodeId nodeId = getNodeId();
        peerStreams.values().forEach(
                stream -> stream.onData(nodeId, event, payload));

    }

    public String getModelName() {
        return sb.getModelName();
    }
}
