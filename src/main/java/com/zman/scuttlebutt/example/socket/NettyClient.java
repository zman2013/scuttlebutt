package com.zman.scuttlebutt.example.socket;

import com.zman.scuttlebutt.Multiplex;
import com.zman.scuttlebutt.Reactor;
import com.zman.scuttlebutt.Stream;
import com.zman.scuttlebutt.Update;
import com.zman.scuttlebutt.example.basic.HashMapModel;
import com.zman.scuttlebutt.example.basic.HashMapModelTester;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import static com.zman.scuttlebutt.Link.link;

public class NettyClient {

    final ClientNettyHandler handler = new ClientNettyHandler();

    public NettyClient() throws InterruptedException {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        new Thread(()-> {
            try {
                Bootstrap b = new Bootstrap();
                b.group(workerGroup);
                b.channel(NioSocketChannel.class);
                b.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(handler);
                    }
                });

                ChannelFuture f = b.connect("localhost", 8081).sync();
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    workerGroup.shutdownGracefully().sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public static void main(String[] args) throws InterruptedException {

        NettyClient nettyClient = new NettyClient();

        Multiplex multiplex = new Multiplex(nettyClient.handler);

        HashMapModel b = new HashMapModel("b");
        Stream sb = b.createStream();

        link(multiplex, sb);

        // 模拟业务线程不断更新model b
        new Thread(()-> {
            while(true) {
                Update[] updates = java.util.stream.Stream.generate(HashMapModelTester::generateSpeedBySin)
                        .limit(1)
                        .toArray(update -> new Update[1]);

                b.applyUpdates(updates);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }).start();

    }

}
