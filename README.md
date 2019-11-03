# scuttlebutt
## Overview
scuttlebutt的java版实现，用来验证该方案落地的可行性，提供了一套基础库，能够便捷的实现自定义数据结构。
## Goals
1. 实现scuttlebutt协议
2. 提供一套简洁的API
3. 多模双工
4. NonBlocking Backpressure
5. 不强依赖三方库
6. 与现有网络框架容易集成
## 自定义Model
```Java
// 继承基类Scuttlebut，实现两个抽象方法
public class HashMapModel extends Scuttlebutt {

    private Map<String, ModelValue> storeMap = new HashMap<>();

    public HashMapModel(String name) {
        nodeId = new NodeId(name);
        clock = new Clock();
    }

    /**
     * 根据对方的时钟计算出delta
     */
    @Override
    public Update[] history(Clock peerClock) {

        return storeMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().timestamp > peerClock.getTimestamp())
                .map(entry -> new Update(entry.getKey(), entry.getValue().value, entry.getValue().timestamp))
                .toArray(Update[]::new);

    }

    /**
     * 对本地model应用updates
     */
    @Override
    public void applyUpdates(Update[] updates) {
        Stream.of(updates)
                .filter(this::obsolete)
                .map(this::applyUpdate)
                .forEach(update -> log.debug("{} apply update: {}", nodeId, update));
    }
    
    ...
```
## Examples
提供了两个例子：
* 进程内model之间数据同步
* 跨网络model之间数据同步
### 1 进程内数据同步
#### 全部代码
```Java
// 创建a、b两个model对象，目标是a有数据更新，自动同步到b
HashMapModel a = new HashMapModel("a");
HashMapModel b = new HashMapModel("b");

// 分别创建一个stream
Stream sa = a.createStream();
Stream sb = b.createStream();

// 关联两个stream
link(sa, sb);
```
#### 模拟业务操作
```Java
// 每秒更新一次a对象
while(true) {
    Update[] updates = java.util.stream.Stream.generate(HashMapModelTester::generateSpeedBySin)
            .limit(1)
            .toArray(update -> new Update[1]);
            
    a.applyUpdates(updates);
    Thread.sleep(1000);
}
```
#### 结果
a产生更新，b自动同步delta
![](https://github.com/zman2013/scuttlebutt/blob/master/output/scuttlebut_inner_process.png) 

### 2 跨网络数据同步
#### 全部代码
Server端  
```Java
// 创建netty server（无侵入，就是普通的NettyServer）
NettyServer nettyServer = new NettyServer();
// 创建reactor模型
Multiplex multiplex = new Multiplex(nettyServer.serverHandler);
// 创建model
HashMapModel a = new HashMapModel("a");
Stream sa = a.createStream();
// 关联stream和multiplex
link(sa, multiplex);
```
Client端  
```Java
// 创建netty client
NettyClient nettyClient = new NettyClient();
// 创建reactor模型
Multiplex multiplex = new Multiplex(nettyClient.handler);
// 创建model
HashMapModel b = new HashMapModel("b");
Stream sb = b.createStream();
// 关联stream和multiplex
link(multiplex, sb);
```
#### 模拟业务操作
```Java
// 每秒操作一次model更新
while(true) {
    Update[] updates = java.util.stream.Stream.generate(HashMapModelTester::generateSpeedBySin)
            .limit(1)
            .toArray(update -> new Update[1]);

    b.applyUpdates(updates);

    Thread.sleep(1000);
}
```
#### 结果
Client产生更新，Server自动同步delta
![](https://github.com/zman2013/scuttlebutt/blob/master/output/scuttlebut_server_client.png)


> https://github.com/jacobbubu/scuttlebutt-pull
> https://github.com/dominictarr/scuttlebutt
> http://www.cs.cornell.edu/home/rvr/papers/flowgossip.pdf



