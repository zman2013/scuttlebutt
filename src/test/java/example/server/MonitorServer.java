package example.server;

import com.zman.net.pull.netty.NettyServer;
import com.zman.pull.stream.IDuplex;
import com.zman.pull.stream.ISink;
import com.zman.pull.stream.impl.DefaultSink;
import com.zman.scuttlebutt.bean.StreamOptions;
import example.BizData;
import example.Model;
import example.through.netty.NettyDecoder;
import example.through.netty.NettyEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.UUID;

import static com.zman.pull.stream.util.Pull.pull;

@Slf4j
public class MonitorServer {

    public static void main(String[] args) throws InterruptedException {

        Model model = new Model(UUID.randomUUID().toString());
        model.on("_update", data -> {
            log.info("applyUpdates: {}", data);
        });

        new NettyServer()
                .onAccept((connectionId, socketDuplex) -> {
                    IDuplex modelDuplex = model.createSbStream(new StreamOptions(true, true));
                    pull(socketDuplex, new NettyDecoder<BizData>(BizData.class), modelDuplex);
                    pull(modelDuplex, new NettyEncoder(), socketDuplex);

                })
                .onDisconnect((connectionId, socketDuplex) -> socketDuplex.close())
                .onThrowable(Throwable::printStackTrace)
                .listen(8081);


        for(int i = 0; i < 10; i ++ ){
            model.set("server-"+i, new Random().nextInt());
            Thread.sleep(1000);
        }

    }

}
