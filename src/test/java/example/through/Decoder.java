package example.through;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zman.pull.stream.ISink;
import com.zman.pull.stream.bean.ReadResult;
import com.zman.pull.stream.bean.ReadResultEnum;
import com.zman.pull.stream.impl.DefaultThrough;
import com.zman.scuttlebutt.SbStream.Outgoing;
import com.zman.scuttlebutt.bean.Update;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Decoder<T> extends DefaultThrough {

    private Status status = Status.StartReadingHead;

    private ByteBuffer headBuffer = ByteBuffer.allocate(4);
    private ByteBuffer bodyBuffer;

    private int contentLength = -1;

    private BlockingQueue updateBuffer = new LinkedBlockingQueue();

    private Type updateType;

    public Decoder(Class<T> dataType){
        updateType = TypeToken.getParameterized(Update.class, dataType).getType();
    }

    public ReadResult get(boolean end, ISink sink) {
        if(updateBuffer.size()>0){
            ReadResult readResult = new ReadResult(updateBuffer.poll());
            return readResult;
        }

        ReadResult readResult = source.get(end, sink);
        if(ReadResultEnum.Available.equals(readResult.status)){
            byte[] data = (byte[]) readResult.data;
            ByteBuffer buf = ByteBuffer.allocate(data.length);
            buf.put(data).flip();

            while(buf.hasRemaining()) {
                switch (status) {
                    case StartReadingHead:
                        if (buf.remaining() >= 4) {
                            contentLength = buf.getInt();

                            status = Status.StartReadingBody;
                        } else {
                            headBuffer.put(buf);

                            status = Status.ContinueReadingHead;
                        }
                        break;
                    case ContinueReadingHead:
                        int remainHeadLength = 4 - headBuffer.position();
                        if (buf.remaining() >= remainHeadLength) {
                            byte[] tmp = new byte[remainHeadLength];
                            buf.get(tmp);
                            headBuffer.put(tmp).flip();

                            contentLength = headBuffer.getInt();

                            status = Status.StartReadingBody;
                        }else{
                            headBuffer.put(buf);
                        }
                        break;
                    case StartReadingBody:
                        bodyBuffer = ByteBuffer.allocate(contentLength);
                        if( buf.remaining() >= contentLength){
                            int dataType = buf.getInt();
                            byte[] tmp = new byte[contentLength-4];
                            buf.get(tmp);
                            if( dataType == 1 ){
                                Update<T> update = gson.fromJson(new String(tmp, StandardCharsets.UTF_8), updateType);
                                updateBuffer.offer(update);
                            }else if(dataType == 2) {
                                Outgoing outgoing = gson.fromJson(new String(tmp, StandardCharsets.UTF_8), Outgoing.class);
                                updateBuffer.offer(outgoing);
                            }else if(dataType == 3 ){
                                updateBuffer.offer(new String(tmp, StandardCharsets.UTF_8));
                            }else{
                                throw new IllegalArgumentException();
                            }

                            reset();
                        }else{
                            byte[] tmp = new byte[buf.remaining()];
                            buf.get(tmp);

                            bodyBuffer.put(tmp);

                            status = Status.ContinueReadingBody;
                        }
                        break;
                    case ContinueReadingBody:
                        if( buf.remaining() >= contentLength - bodyBuffer.position()){
                            byte[] tmp = new byte[contentLength-bodyBuffer.position()];
                            buf.get(tmp);
                            bodyBuffer.put(tmp).flip();

                            int dataType = bodyBuffer.getInt();
                            tmp = new byte[contentLength - 4];
                            bodyBuffer.get(tmp);
                            if( dataType == 1 ) {
                                Update<T> update = gson.fromJson(new String(tmp, StandardCharsets.UTF_8), updateType);
                                updateBuffer.offer(update);
                            }else if(dataType == 2){
                                Outgoing outgoing = gson.fromJson(new String(tmp, StandardCharsets.UTF_8), Outgoing.class);
                                updateBuffer.offer(outgoing);
                            }else if(dataType == 3 ){
                                updateBuffer.offer(new String(tmp, StandardCharsets.UTF_8));
                            }else{
                                throw new IllegalArgumentException();
                            }

                            reset();
                        }else{
                            byte[] tmp = new byte[buf.remaining()];
                            buf.get(tmp);

                            bodyBuffer.put(tmp);
                        }
                        break;
                }
            }

            if(updateBuffer.size()>0){
                readResult = new ReadResult(updateBuffer.poll());
            }

        }

        return readResult;
    }

    private void reset(){
        headBuffer.clear();
        bodyBuffer.clear();
        status = Status.StartReadingHead;
    }

    private Gson gson = new Gson();

    enum Status{
        StartReadingHead,
        ContinueReadingHead,
        StartReadingBody,
        ContinueReadingBody
    }

}
