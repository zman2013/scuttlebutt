package com.zman.scuttlebutt;

import com.zman.event.EventEmitter;
import com.zman.monotonic.timestamp.Timestamp;
import com.zman.pull.stream.IDuplex;
import com.zman.scuttlebutt.bean.StreamOptions;
import com.zman.scuttlebutt.bean.Update;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Scuttlebutt 同步Model的基类
 * 维护一份已接收的所有Update的Source的最新时钟：sources
 *
 * 提供创建Stream（即SbStream）的方法
 * 并负责将Update广播到所有stream上
 *
 * 提供了applyUpdate和history抽象方法，具体由子类Model实现
 *
 */
@Slf4j
public abstract class Scuttlebutt extends EventEmitter {

    /**
     * 该scuttlebutt的唯一id
     */
    String id;

    /**
     * 该scuttlebutt上创建的stream个数
     */
    int streams;

    /**
     * 此节点掌握的全部知识，即：sourceId -> timestamp
     *
     * QA：
     * sources初始化为空，一个节点每次上线都会接收到相邻接点的history
     * 触发model重复applyUpdate
     * 1. 持久化解决
     * 2. model自己处理幂等
     */
    @Getter
    private Map<String, Long> sources = new HashMap<>();


    public Scuttlebutt(){
        this(UUID.randomUUID().toString());
    }


    public Scuttlebutt(String id){
        this.id = id;
    }


    /**
     * 将update应用到model
     * @param update 更新
     * @return 更新成功失败
     */
    public abstract <T> boolean applyUpdate(Update<T> update);


    /**
     * 根据对方向量钟（peerClocks）计算与本节点的deltaList
     * @param sources 对方向量钟
     * @return deltaList
     */
    public abstract <T> Update<T>[] history(Map<String, Long> sources);


    /**
     * 更新本地model应调用localUpdate方法
     * localUpdate会将model层的数据结构转化为update，并交给update处理
     * update方法会回调model层的applyUpdate，进而将update应用到model
     * @param data
     * @return
     */
    protected <T> boolean localUpdate(T data){
        return this.update(new Update<>(data, Timestamp.uniq(), this.id, this.id));
    }


    /**
     * 创建一个stream
     * @return
     */
    public IDuplex createSbStream(){
        StreamOptions streamOptions = new StreamOptions();
        streamOptions.setReadable(true);
        streamOptions.setWritable(true);
        return createSbStream(streamOptions);
    }

    /**
     * 创建一个stream
     * @return
     */
    public IDuplex createSbStream(StreamOptions streamOptions){
        SbStream sbStream = new SbStream(this, streamOptions);
        return sbStream.getDuplex();
    }

    /**
     * localUpdate 和 stream上的update会触发此方法
     * @param update
     * @return
     */
    <T> boolean update(Update<T> update){
        long timestamp = update.timestamp;
        String sourceId = update.sourceId;

        long latestTimestamp = sources.computeIfAbsent(sourceId, (key)->0L);

        if( latestTimestamp >= timestamp ){
            log.info("{} update is older, ignore it: {}", id, update);
            return false;
        }

        boolean success = this.applyUpdate(update);

        if(success) {
            this.sources.put(sourceId, timestamp);
            log.debug("{} update our clock to: {}", id, sources);
        }else{
            log.error("{} apply update failed: {}", id, update);
        }

        // 即使本地更新失败了，依然可以把update广播到stream中
        emit("_update", update);

        return success;
    }



}
