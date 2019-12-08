package example.client;

import com.zman.pull.stream.IDuplex;
import com.zman.pull.stream.ISource;
import com.zman.scuttlebutt.bean.StreamOptions;
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
public class MonitorClient {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {

        Model model = new Model("client");
        model.on("updated", data -> log.info("model updated: {}", data));

        IDuplex source = model.createSbStream(new StreamOptions(true, true));

        for(int i = 0; i < 10; i ++ ){
            model.set("key-"+i, new Random().nextInt());
        }

        EventLoop eventLoop = new DefaultEventLoop();
        new SocketClient(eventLoop)
                .onConnected(duplex->{
                    log.info("connected duplex: {}", duplex);
                    pull( pull(source, new SbEncoder(BizData.class), new SocketEncoder()),
                            duplex);
                    ISource iSource = pull(duplex, new SocketDecoder(), new SbDecoder(BizData.class));
                    pull(iSource, source);
                })
                .onDisconnected(()-> log.info("disconnected"))
                .onThrowable(Throwable::printStackTrace)
                .connect("localhost", 8081);


//        for(int i = 0; i < 10; i ++ ){
//            model.set("key-"+i, new Random().nextInt());
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

        System.in.read();
    }

}
