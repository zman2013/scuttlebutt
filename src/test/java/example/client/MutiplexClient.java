package example.client;

import com.zman.pull.stream.IDuplex;
import com.zman.pull.stream.ISink;
import com.zman.scuttlebutt.bean.StreamOptions;
import com.zman.stream.multiplex.pull.IChannel;
import com.zman.stream.multiplex.pull.IMultiplex;
import com.zman.stream.multiplex.pull.codec.MultiplexDecoder;
import com.zman.stream.multiplex.pull.codec.MultiplexEncoder;
import com.zman.stream.multiplex.pull.impl.DefaultMultiplex;
import com.zman.stream.socket.pull.SocketClient;
import com.zman.stream.socket.pull.codec.SocketDecoder;
import com.zman.stream.socket.pull.codec.SocketEncoder;
import com.zman.thread.eventloop.EventLoop;
import com.zman.thread.eventloop.impl.DefaultEventLoop;
import example.BizData;
import example.Model;
import example.codec.SbDecoder;
import example.codec.SbEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Random;

import static com.zman.pull.stream.util.Pull.pull;

@Slf4j
public class MutiplexClient {


    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {

        Model model = new Model("client");
        model.on("updated", data -> log.info("model updated: {}", data));

        IDuplex source = model.createSbStream(new StreamOptions(true, true));

        IMultiplex multiplex = new DefaultMultiplex();
        IChannel localChannel = multiplex.createChannel("food-model");
        pull(source, new SbEncoder(BizData.class), localChannel.duplex());
        pull(localChannel.duplex(), new SbDecoder(BizData.class), source);

        new SocketClient(new DefaultEventLoop())
                .onConnected(socketDuplex->{
                    log.info("connected duplex: {}", socketDuplex);

                    socketDuplex.sink().onClosed(System.out::println);
                    socketDuplex.source().onClosed(System.out::println);

                    pull( pull(multiplex.duplex(), new MultiplexEncoder(), new SocketEncoder()),
                            socketDuplex);

                    pull( pull(socketDuplex, new SocketDecoder(), new MultiplexDecoder()),
                            multiplex.duplex());
                })
                .onDisconnected(()-> log.info("disconnected"))
                .onThrowable(Throwable::printStackTrace)
                .connect("localhost", 8081);


        for(int i = 0; i < 10; i ++ ){
            model.set("client-key-"+i, new Random().nextInt());
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.in.read();
    }

}
