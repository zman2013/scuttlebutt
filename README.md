# scuttlebutt
## Overview
scuttlebutt的java版实现，用来验证该方案落地的可行性，提供了一套基础库，能够便捷的实现自定义数据结构。
## Goals
1. 实现scuttlebutt协议
2. 提供一套简洁的API
3. 多模双工
4. pull stream
5. 不强依赖三方库
6. 与现有网络框架容易集成
## 自定义Model
```Java
// 继承基类Scuttlebut，实现两个抽象方法
public class Model extends Scuttlebutt {
    @Override
    public boolean applyUpdate(Update update) {

        String key = ((BizData)update.data).key;

        if( store.computeIfAbsent(key,(k)->new Update()).timestamp > update.timestamp ){
            log.info("I have a more recent one: {}", update);
            return true;
        }

        store.put(key, update);
        // emit changes events

        return true;
    }

    @Override
    public Update[] history(Map<String, Long> sources) {

        return store.values().stream()
                .filter( update -> {
                    if( sources.computeIfAbsent(update.sourceId, (s)->0L) < update.timestamp ){
                        return true;
                    }else{
                        return false;
                    }
                })
                .toArray(Update[]::new);

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
Model a = new Model("a");
Model b = new Model("b");

a.set("a-key1", "hello world");
b.set("b-key2", "hello universe");

log.info("");
log.info("######## begin #########");
log.info(a.toString());
log.info(b.toString());

log.info("");
log.info("######## link ########");

Duplex sa = a.createSbStream();
Duplex sb = b.createSbStream();

sa.sink(sb::source);
sb.sink(sa::source);

log.info("");
log.info("######## finally ########");

log.info("a -> {}", a.toString());
log.info("b -> {}", b.toString());
```

## 历史版本
0.1 -> https://github.com/zman2013/scuttlebutt/tree/0.1

## 引用
> https://github.com/jacobbubu/scuttlebutt-pull  
> https://github.com/dominictarr/scuttlebutt  
> http://www.cs.cornell.edu/home/rvr/papers/flowgossip.pdf



