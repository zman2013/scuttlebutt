package com.zman.scuttlebutt;


import lombok.Getter;
import lombok.Setter;

@Setter@Getter
public abstract class Scuttlebutt {

    /**
     * 自己的时钟
     */
    protected Clock clock;

    /**
     * 节点唯一id
     */
    protected NodeId nodeId;

    /**
     * model名字，全局唯一
     */
    public String getModelName(){return "whole";}

    /**
     * 应用updates
     * @param updates
     */
    public abstract void applyUpdates(Update[] updates);


    /**
     * 根据对方的时钟计算出delta
     * @param peerClock
     */
    public abstract Update[] history(Clock peerClock);


    /**
     * 从model中创建流
     * @return
     */
    public Stream createStream(){

        return new Stream(this);
    }



}
