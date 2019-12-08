package example.codec;

import com.zman.pull.stream.IDuplex;
import com.zman.scuttlebutt.bean.StreamOptions;
import com.zman.stream.multiplex.pull.IChannel;
import com.zman.stream.multiplex.pull.IMultiplex;
import com.zman.stream.multiplex.pull.impl.DefaultMultiplex;
import example.BizData;
import example.Model;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.zman.pull.stream.util.Pull.link;
import static com.zman.pull.stream.util.Pull.pull;

@Slf4j
public class CodecTest {


    public static void main(String[] args){
        // client
        Model model = new Model("client");
        IDuplex source = model.createSbStream(new StreamOptions(true, true));

        IMultiplex multiplexClient = new DefaultMultiplex();
        IChannel localChannel = multiplexClient.createChannel("food-model");
        pull(source, new SbEncoder(BizData.class), localChannel.duplex());
        pull(localChannel.duplex(), new SbDecoder(BizData.class), source);


        // server
        Model modelServer = new Model(UUID.randomUUID().toString());
        modelServer.on("_update", data -> log.info("server applyUpdates: {}", data));

        IMultiplex multiplexServer = new DefaultMultiplex()
                .onAccept(channel -> {

                    log.info("receive remote channel: id:{}, resourceId:{}", channel.id(), channel.resourceId());

                    IDuplex modelDuplex = modelServer.createSbStream(new StreamOptions(true, true));

                    pull(channel.duplex(), new SbDecoder(BizData.class), modelDuplex);

                    pull(modelDuplex, new SbEncoder(BizData.class), channel.duplex());
                });

        // link
        link(multiplexClient.duplex(), multiplexServer.duplex());
    }

}
