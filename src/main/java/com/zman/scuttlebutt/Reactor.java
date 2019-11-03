package com.zman.scuttlebutt;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
public class Reactor {

    /**
     * modelName -> stream list
     * <p>
     * 这里数据传输的最小粒子为model，其实可以拆到更细的维度
     */
    private Map<String, List<Stream>> modelStreamMap = new ConcurrentHashMap<>();

    private IOHandler ioHandler;

    public Reactor(IOHandler ioHandler) {
        this.ioHandler = ioHandler;
        ioHandler.onListen(this::onListen);
    }

    public void register(Stream stream) {
        modelStreamMap.computeIfAbsent(stream.getModelName(), k -> new ArrayList<>())
                .add(stream);
    }

    /**
     * 收到来自IO的消息
     */
    private Gson gson = new Gson();

    public void onListen(byte[] data) {

        String[] fields = gson.fromJson(new String(data, StandardCharsets.UTF_8), String[].class);
        String modelName = fields[0];
        ScuttlebuttEvent event = ScuttlebuttEvent.valueOf(fields[1]);
        NodeId nodeId = new NodeId(fields[2]);

        switch (event) {
            case UPDATE:
                Object payload = gson.fromJson(fields[3], Update[].class);
                modelStreamMap.computeIfAbsent(modelName, k -> new ArrayList<>())
                        .forEach(stream -> stream.onData(nodeId, event, payload));
                break;
            case REQUEST:
                Object clock = gson.fromJson(fields[3], Clock.class);
                modelStreamMap.computeIfAbsent(modelName, k -> new ArrayList<>())
                        .forEach(stream -> stream.onData(nodeId, event, clock));
                break;
            default:
                Object other = fields[3];
                modelStreamMap.computeIfAbsent(modelName, k -> new ArrayList<>())
                        .forEach(stream -> stream.onData(nodeId, event, other));
        }

    }

    /**
     * 将数据发送到io链路层
     *
     * @param modelName
     * @param event
     * @param nodeId
     * @param payload
     */
    public void sendMessage(NodeId nodeId, String modelName, ScuttlebuttEvent event, Object payload) {

        String[] fields = new String[]{
                modelName,
                event.name(),
                nodeId.getName(),
                gson.toJson(payload)
        };

        String data = gson.toJson(fields);

        ioHandler.send(data.getBytes(StandardCharsets.UTF_8));
    }

}
