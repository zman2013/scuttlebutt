package example;

import com.zman.pull.stream.IDuplex;
import com.zman.pull.stream.util.Pull;
import com.zman.scuttlebutt.Scuttlebutt;
import com.zman.scuttlebutt.bean.Update;
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
            return false;
        }

        store.put(key, update);
        // emit changes events

        return true;
    }

    @Override
    public <T> Update<T>[] history(Map<String, Long> sources) {

        return store.values().stream()
                .filter( update -> sources.computeIfAbsent(update.sourceId, (s) -> 0L) < update.timestamp)
                .sorted((a, b) -> {
                    if(a.timestamp >b.timestamp){
                        return 1;
                    }else if(a.timestamp < b.timestamp){
                        return -1;
                    }else{
                        return 0;
                    }
                })
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

        IDuplex sa = a.createSbStream();
        IDuplex sb = b.createSbStream();

        Pull.link(sa, sb);

        a.set("a-key2", "hahaha");

        log.info("");
        log.info("######## finally ########");

        log.info("a -> {}", a.toString());
        log.info("b -> {}", b.toString());
    }
}
