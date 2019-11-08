package com.zman.scuttlebutt.example;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public abstract class Scuttlebutt extends EventEmitter{


    public int streams;

    public Map<String, Long> sources = new HashMap<>();

    public String id;


    public Scuttlebutt(){
        this(UUID.randomUUID().toString());
    }

    public Scuttlebutt(String id){
        this.id = id;
    }

    public abstract boolean applyUpdate(Update update);

    public abstract Update[] history(Map<String, Long> sources);

    public boolean localUpdate(Object data){
        return this.update(new Update(data, System.currentTimeMillis(), this.id, this.id));
    }

    public Duplex createSbStream(){
        return new SbStream(this).duplex;
    }

    /**
     * localUpdate 和 history会触发此方法
     * @param update
     * @return
     */
    private boolean update(Update update){
        long timestamp = update.timestamp;
        String sourceId = update.sourceId;

        long latestTimestamp = sources.computeIfAbsent(sourceId, (key)->0L);

        if( latestTimestamp >= timestamp ){
            log.info("udpate is older, ignore it: {}", update);
            return false;
        }

        this.sources.put(sourceId, timestamp);
        log.debug("{} update our sources to: {}", id, sources);

        boolean result = this.applyUpdate(update);

        emit("_update", update);

        return result;
    }


    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Update {
        public Object data;

        public long timestamp;

        public String sourceId;

        public String from;


    }
}
