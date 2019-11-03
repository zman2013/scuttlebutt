package com.zman.scuttlebutt.example.socket;

import com.zman.scuttlebutt.Multiplex;
import com.zman.scuttlebutt.Reactor;
import com.zman.scuttlebutt.Stream;
import com.zman.scuttlebutt.example.basic.HashMapModel;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

import static com.zman.scuttlebutt.Link.link;

public class NettyServer {

    final ServerNettyHandler serverHandler = new ServerNettyHandler();

    public NettyServer() {
        new Thread(() -> {
            EventLoopGroup group = new NioEventLoopGroup();

            try{
                ServerBootstrap b = new ServerBootstrap();
                b.group(group)
                        .channel(NioServerSocketChannel.class)
                        .localAddress(new InetSocketAddress(8081))
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) {
                                socketChannel.pipeline().addLast(serverHandler);
                            }
                        });

                ChannelFuture f = b.bind().sync();
                f.channel().closeFuture().sync();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    group.shutdownGracefully().sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }){}.start();

    }


    public static void main(String[] args){
        NettyServer nettyServer = new NettyServer();

        Multiplex multiplex = new Multiplex(nettyServer.serverHandler);

        HashMapModel a = new HashMapModel("a");
        Stream sa = a.createStream();

        link(sa, multiplex);

    }

}
