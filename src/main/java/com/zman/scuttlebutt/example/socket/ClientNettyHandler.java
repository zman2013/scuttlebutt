package com.zman.scuttlebutt.example.socket;

import com.zman.scuttlebutt.IOHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
@Sharable
public class ClientNettyHandler extends ChannelInboundHandlerAdapter implements IOHandler {

    private Channel channel;

    private Consumer<byte[]> consumer;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        ByteBuf in = (ByteBuf) msg;

        if( in.readableBytes() >= 4 ) {
            int length = in.readInt();

            if (in.readableBytes() >= length) {
                byte[] bytes = new byte[length];
                in.readBytes(bytes);

                log.debug("Server received: " + new String(bytes, StandardCharsets.UTF_8));

                consumer.accept(bytes);

                log.debug(ctx.channel().toString());
            }
        }

    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.channel = ctx.channel();

        log.debug( ctx.channel() + " active" );

    }

    @Override
    public void send(byte[] bytes) {
        if(channel != null ) {
            log.debug("sending data, length: {}", bytes.length);
            ByteBuf b = Unpooled.buffer(4);
            b.writeInt(bytes.length);
            channel.write(b);

            ByteBuf buf = Unpooled.copiedBuffer(bytes);
            channel.write(buf);
            channel.flush();
            log.debug("sent, length: {}", bytes.length);
        }else{
            log.debug("no remote peer");
        }
    }

    @Override
    public void onListen(Consumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        this.channel = null;
    }
}
