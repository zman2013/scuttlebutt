package example.server;

import com.zman.net.pull.netty.NettyServer;
import com.zman.net.pull.netty.codec.NettyDecoder;
import com.zman.net.pull.netty.codec.NettyEncoder;
import com.zman.pull.stream.IDuplex;
import com.zman.pull.stream.ISink;
import com.zman.pull.stream.bean.ReadResult;
import com.zman.pull.stream.impl.DefaultThrough;
import com.zman.scuttlebutt.bean.StreamOptions;
import example.BizData;
import example.Model;
import example.codec.SbDecoder;
import example.codec.SbEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.UUID;

import static com.zman.pull.stream.util.Pull.pull;

@Slf4j
public class MonitorServer {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws InterruptedException {

        Model model = new Model(UUID.randomUUID().toString());
        model.on("_update", data -> {
            log.info("applyUpdates: {}", data);
        });

        new NettyServer()
                .onAccept((connectionId, socketDuplex) -> {
                    IDuplex modelDuplex = model.createSbStream(new StreamOptions(true, true));
                    pull( pull(socketDuplex, new NettyDecoder(), new SbDecoder(BizData.class)),
                            modelDuplex);
                    pull( pull(modelDuplex, new SbEncoder(BizData.class), new NettyEncoder(),
                            new DefaultThrough(){
                                @Override
                                public ReadResult get(boolean end, Throwable throwable, ISink sink) {
                                    ReadResult readResult = super.get(end, throwable, sink);
                                    System.out.println(readResult.status.name());
                                    return readResult;
                                }
                            }
                            ),
                            socketDuplex);
                })
                .onDisconnect((connectionId, socketDuplex) -> socketDuplex.close())
                .onThrowable(Throwable::printStackTrace)
                .listen(8081);


        for(int i = 0; i < 10009; i ++ ){
            model.set("server-"+i, new Random().nextInt());
            Thread.sleep(1000);
        }

    }

}
