package com.zman.scuttlebutt.example.basic.async;

import com.zman.scuttlebutt.example.AsyncScuttlebutt;
import com.zman.scuttlebutt.example.Duplex;
import com.zman.scuttlebutt.example.basic.BizData;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@ToString
public class AsyncModel extends AsyncScuttlebutt {

    public Map<String, Update> store = new HashMap<>();

    public AsyncModel(String id) {
        super(id);
    }


    public CompletableFuture<Boolean> set(String key, Object value){
        return CompletableFuture.supplyAsync(() -> {

            try {
                reentrantLock(() -> localUpdate(new BizData(key, value)));
            }catch (Throwable throwable){return false;}

            return true;

        });
    }

    public CompletableFuture<BizData> get(String key){
        return CompletableFuture.supplyAsync(() ->
                (BizData)store.get(key).data);
    }


    @Override
    public boolean applyUpdate(Update update) {

        String key = ((BizData)update.data).key;

        if( update.timestamp < store.computeIfAbsent(key,(k)->new Update()).timestamp  ){
            log.info("I have a more recent one : {}", update);
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

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        AsyncModel a = new AsyncModel("a");
        AsyncModel b = new AsyncModel("b");

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

        Future f = a.set("a-key2", "hahaha");

        log.info("");
        log.info("######## finally ########");

        log.info("a -> {}", a.toString());
        log.info("b -> {}", b.toString());

        f.get();
        log.info("");
        log.info("######## real finally ########");

        log.info("a -> {}", a.toString());
        log.info("b -> {}", b.toString());
    }

}
