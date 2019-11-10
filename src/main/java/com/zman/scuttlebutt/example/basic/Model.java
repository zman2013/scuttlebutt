package com.zman.scuttlebutt.example.basic;

import com.zman.scuttlebutt.example.Duplex;
import com.zman.scuttlebutt.example.Scuttlebutt;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Model extends Scuttlebutt {

    public Map<String, Update> store = new HashMap<>();

    public Model(String id) {
        super(id);
    }


    public void set(String key, Object value){
        localUpdate(new BizData(key, value));
    }

    public BizData get(String key){
        return (BizData) store.get(key).data;
    }

    @Override
    public boolean applyUpdate(Update update) {

        String key = ((BizData)update.data).key;

        if( store.computeIfAbsent(key,(k)->new Update()).timestamp > update.timestamp ){
            log.info("I have a more recent one: {}", update);
            return true;
        }

        store.put(key, update);
        // emit changes events

        return true;
    }

    @Override
    public Update[] history(Map<String, Long> sources) {

        return store.values().stream()
                .filter( update -> sources.computeIfAbsent(update.sourceId, (s) -> 0L) < update.timestamp)
                .sorted((a, b) -> (int)(a.timestamp - b.timestamp))
                .toArray(Update[]::new);
    }

    @Override
    public String toString() {

        StringBuilder s = new StringBuilder();
        store.values()
                .forEach(value -> {
                    s.append(value.data).append(",");
                });
        return s.toString();
    }

    public static void main(String[] args){
        Model a = new Model("a");
        Model b = new Model("b");

        a.set("a-key1", "hello world");
        b.set("b-key2", "hello universe");

        log.info("");
        log.info("######## begin #########");
        log.info(a.toString());
        log.info(b.toString());

        log.info("");
        log.info("######## link ########");

        Duplex sa = a.createSbStream();
        Duplex sb = b.createSbStream();

        sa.sink(sb::source);
        sb.sink(sa::source);

        a.set("a-key2", "hahaha");

        log.info("");
        log.info("######## finally ########");

        log.info("a -> {}", a.toString());
        log.info("b -> {}", b.toString());
    }
}
