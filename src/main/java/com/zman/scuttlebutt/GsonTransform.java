package com.zman.scuttlebutt;

public class GsonTransform extends Stream {

    @Override
    public void link(Stream peerStream) {
        NodeId nodeId = peerStream.getNodeId();
        super.peerStreams.put(nodeId, peerStream);
    }

    @Override
    public void onData(NodeId peerNodeId, ScuttlebuttEvent event, Object payload) {
        super.onData(peerNodeId, event, payload);
    }
}
