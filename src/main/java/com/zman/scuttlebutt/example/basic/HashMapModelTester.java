package com.zman.scuttlebutt.example.basic;

import com.zman.scuttlebutt.Stream;
import com.zman.scuttlebutt.Update;

import static com.zman.scuttlebutt.Link.link;

public class HashMapModelTester {


    public static void main(String[] args) {
        HashMapModel a = new HashMapModel("a");
        HashMapModel b = new HashMapModel("b");

        Stream sa = a.createStream();
        Stream sb = b.createStream();

        link(sa, sb);

        // 模拟业务线程不断更新model a
        new Thread(()-> {
            while(true) {
                Update[] updates = java.util.stream.Stream.generate(HashMapModelTester::generateSpeedBySin)
                        .limit(1)
                        .toArray(update -> new Update[1]);

                a.applyUpdates(updates);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }).start();

        // 模拟业务线程不断从model b中读取数据
//        new Thread(()-> {
//            while(true) {
//                b.getStoreMap().entrySet()
//                        .forEach(System.out::println);
//
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException ignored) {}
//            }
//        }).start();

//        sa.close();
//        sb.close();
//
//        System.out.println("end");
    }


    public static Update generateSpeedBySin() {
        long timestamp = System.currentTimeMillis();
        String speed = String.valueOf(Math.abs(Math.sin(timestamp/10000d)*120));
        return new Update("speed", speed, timestamp);
    }

}
