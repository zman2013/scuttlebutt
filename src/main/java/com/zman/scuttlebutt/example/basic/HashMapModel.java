package com.zman.scuttlebutt.example.basic;

import com.zman.scuttlebutt.Clock;
import com.zman.scuttlebutt.NodeId;
import com.zman.scuttlebutt.Scuttlebutt;
import com.zman.scuttlebutt.Update;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Getter
public class HashMapModel extends Scuttlebutt {

    private Map<String, ModelValue> storeMap = new HashMap<>();

    public HashMapModel(String name) {
        nodeId = new NodeId(name);
        clock = new Clock();
    }

    @Override
    public Update[] history(Clock peerClock) {

        return storeMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().timestamp > peerClock.getTimestamp())
                .map(entry -> new Update(entry.getKey(), entry.getValue().value, entry.getValue().timestamp))
                .toArray(Update[]::new);

    }

    @Override
    public void applyUpdates(Update[] updates) {
        Stream.of(updates)
                .filter(this::obsolete)
                .map(this::applyUpdate)
                .forEach(update -> log.debug("{} apply update: {}", nodeId, update));
    }

    /**
     * 判断update是否应被废弃
     *
     * @param update
     * @return
     */
    private boolean obsolete(Update update) {
        String attr = update.getAttribute();
        ModelValue modelValue = storeMap.get(attr);
        // update的时间戳 早于 本地更新的时间戳
        if (modelValue != null && update.getTimestamp() < modelValue.timestamp) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 应用update到本地model
     *
     * @return
     */
    private Update applyUpdate(Update update) {
        String attr = update.getAttribute();
        storeMap.put(attr, new ModelValue(update.getValue(), update.getTimestamp()));
        setClock(new Clock(update.getTimestamp()));

        return update;
    }

    @AllArgsConstructor
    @ToString
    public class ModelValue {
        String value;
        long timestamp;
    }
}
