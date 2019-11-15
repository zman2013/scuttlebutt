package com.zman.scuttlebutt;

import java.util.concurrent.locks.ReentrantLock;

public abstract class AsyncScuttlebutt extends Scuttlebutt {

    /**
     * model modify lock
     */
    private ReentrantLock reentrantLock = new ReentrantLock();

    public AsyncScuttlebutt(String id) {
        super(id);
    }


    public void reentrantLock(Runnable runnable){

        try{
            reentrantLock.tryLock();

            runnable.run();

        }finally {
            reentrantLock.unlock();
        }
    }


}
