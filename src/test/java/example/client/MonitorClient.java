package example.client;

import com.zman.pull.stream.IDuplex;
import com.zman.scuttlebutt.bean.StreamOptions;
import com.zman.stream.socket.pull.SocketClient;
import com.zman.thread.eventloop.EventLoop;
import com.zman.thread.eventloop.impl.DefaultEventLoop;
import example.BizData;
import example.Model;
import example.through.Decoder;
import example.through.Encoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Random;

import static com.zman.pull.stream.util.Pull.pull;

@Slf4j
public class MonitorClient {


    public static void main(String[] args) throws IOException {

        Model model = new Model("client");
        IDuplex source = model.createSbStream(new StreamOptions(true, true));

        for(int i = 0; i < 10; i ++ ){
            model.set("key-"+i, new Random().nextInt());
        }

        EventLoop eventLoop = new DefaultEventLoop();
        new SocketClient(eventLoop)
                .onConnected(duplex->{
                    log.info("connected duplex: {}", duplex);
                    pull(source, new Encoder(), duplex);
                    pull(duplex, new Decoder(BizData.class), source);
                })
                .onDisconnected(()-> log.info("disconnected"))
                .onThrowable(Throwable::printStackTrace)
                .connect("localhost", 8081);


//        for(int i = 0; i < 1000; i ++ ){
//            model.set("key-"+i, new Random().nextInt());
//        }

        System.in.read();
    }

}
