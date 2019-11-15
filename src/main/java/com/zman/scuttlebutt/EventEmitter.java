package com.zman.scuttlebutt;

import java.util.*;
import java.util.function.Consumer;

public class EventEmitter {

    private Map<String, Set<Consumer>> eventListener = new HashMap<>();

    public void emit(String event, Object data){
        eventListener.computeIfAbsent(event,(e)-> new HashSet<>()).forEach(f -> f.accept(data));
    }

    public void on(String event, Consumer function){
        eventListener.computeIfAbsent(event, (e)->new HashSet<>()).add(function);
    }

    public void removeListener(String event, Consumer function){
        eventListener.get(event).remove(function);
    }

}
