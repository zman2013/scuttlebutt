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
## Examples
提供了两个例子：
* 进程内model之间数据同步
* 跨网络model之间数据同步
### 1 进程内数据同步
#### 全部代码
```
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
```
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

### 2 跨网络数据同步
#### 全部代码
Server端  
```
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
```
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
```
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
Server端  

Client端  







