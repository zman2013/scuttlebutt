package com.zman.scuttlebutt;

import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
public class Multiplex extends Stream{

    private Reactor reactor;

    private NodeId nodeId;

    public Multiplex(IOHandler ioHandler){

        this.reactor = new Reactor(ioHandler);
        this.nodeId = new NodeId(UUID.randomUUID().toString());
    }


    public void link(Stream stream){
        reactor.register(stream);
    }

    /**
     * modelName为全量
     *
     * @param event
     * @param peerNodeId
     * @param payload
     */
    @Override
    public void onData(NodeId peerNodeId, ScuttlebuttEvent event, Object payload) {
        reactor.sendMessage(peerNodeId, "whole", event, payload);
    }

    @Override
    public NodeId getNodeId() {
        return nodeId;
    }
}
