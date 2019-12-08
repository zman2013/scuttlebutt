package example.server;

import com.zman.net.pull.netty.NettyServer;
import com.zman.net.pull.netty.codec.NettyDecoder;
import com.zman.net.pull.netty.codec.NettyEncoder;
import com.zman.pull.stream.IDuplex;
import com.zman.scuttlebutt.bean.StreamOptions;
import com.zman.stream.multiplex.pull.IMultiplex;
import com.zman.stream.multiplex.pull.codec.MultiplexDecoder;
import com.zman.stream.multiplex.pull.codec.MultiplexEncoder;
import com.zman.stream.multiplex.pull.impl.DefaultMultiplex;
import example.BizData;
import example.Model;
import example.codec.SbDecoder;
import example.codec.SbEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.zman.pull.stream.util.Pull.pull;

@Slf4j
public class MultiplexServer {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {

        Model model = new Model(UUID.randomUUID().toString());
        model.on("updated", data -> log.info("model updated: {}", data));


        NettyServer nettyServer = new NettyServer();
        nettyServer.onAccept((connectionId, socketDuplex) -> {

                    log.info("received connection id: {}", connectionId);

                    IMultiplex multiplex = createMultiplex(model);

                    pull(pull(socketDuplex.source(), new NettyDecoder(), new MultiplexDecoder()),
                            multiplex.duplex().sink());

                    pull(pull(multiplex.duplex().source(), new MultiplexEncoder(), new NettyEncoder()),
                            socketDuplex.sink());
                });
        nettyServer.onDisconnect((connectionId, socketDuplex) -> socketDuplex.close());
        nettyServer.onThrowable(Throwable::printStackTrace)
            .listen(8081);


//        for(int i = 0; i < 10; i ++ ){
//            model.set("server-"+i, new Random().nextInt());
//            Thread.sleep(1000);
//        }

    }


    private static IMultiplex createMultiplex(Model model) {
        return new DefaultMultiplex()
                .onAccept(channel -> {

                    log.info("receive remote channel: id:{}, resourceId:{}", channel.id(), channel.resourceId());

                    IDuplex modelDuplex = model.createSbStream(new StreamOptions(true, true));

                    pull(channel.duplex(), new SbDecoder(BizData.class), modelDuplex);

                    pull(modelDuplex, new SbEncoder(BizData.class), channel.duplex());
                })
                .onClosed(throwable -> {if(throwable!=null)throwable.printStackTrace();})
                ;
    }

}
