package com.zman.scuttlebutt.example;

import java.util.*;
import java.util.function.Function;

public class EventEmitter {

    private Map<String, Set<Function>> eventListener = new HashMap<>();

    public void emit(String event, Object data){
        eventListener.computeIfAbsent(event,(e)-> new HashSet<>()).forEach(f -> f.apply(data));
    }

    public void on(String event, Function function){
        eventListener.computeIfAbsent(event, (e)->new HashSet<>()).add(function);
    }

    public void removeListener(String event, Function function){
        eventListener.get(event).remove(function);
    }

}
