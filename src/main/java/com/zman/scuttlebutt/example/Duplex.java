package com.zman.scuttlebutt.example;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@Slf4j
public class Duplex {

    public boolean readable = true;

    public ArrayBlockingQueue buffer = new ArrayBlockingQueue(1000);

    private String name;

    private UnaryOperator<Object> onData;

    private ArrayBlockingQueue<Supplier> promiseList = new ArrayBlockingQueue(1000);

    public Duplex(String name, UnaryOperator onData, Object onClose) {
        this.name = name;
        this.onData = onData;
    }


    public void push(Object outgoing) {
        buffer.add(outgoing);

        if( !promiseList.isEmpty() ) {
            while (!promiseList.isEmpty()
                    && !buffer.isEmpty()) { // 很重要
                log.debug("{} invoke promise, promise number: {}", name, promiseList.size());
                promiseList.poll().get();
            }
        }

    }


    public void source(boolean end, BiConsumer<Boolean, Object> cb){
        if(end){
            cb.accept(end, null);
            return;
        }

        if( buffer.isEmpty() ){

            Supplier promise = () -> {      // 很重要
                Object obj = buffer.poll();
                cb.accept(false, obj);
                return obj;
            };
            log.debug("{} record promise", name);
            promiseList.add(promise);

            return;
        }

        cb.accept(false, buffer.poll());
    }


    public static class Recursive<I> {
        public I func;
    }
    public void sink(BiConsumer<Boolean, BiConsumer<Boolean, Object>> read){

        Recursive<BiConsumer<Boolean, Object>> recursive = new Recursive<>();
        recursive.func = (end, data) -> {
            if( end )   return;

            onData.apply(data);

            read.accept(false, recursive.func);
        };

        read.accept(false, recursive.func);
    }
}
