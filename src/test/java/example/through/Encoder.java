package example.through;

import com.google.gson.Gson;
import com.zman.pull.stream.ISink;
import com.zman.pull.stream.bean.ReadResult;
import com.zman.pull.stream.bean.ReadResultEnum;
import com.zman.pull.stream.impl.DefaultThrough;
import com.zman.scuttlebutt.SbStream.Outgoing;
import com.zman.scuttlebutt.bean.Update;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Encoder extends DefaultThrough {

    private ByteBuffer byteBuffer;

    private Gson gson = new Gson();

    @Override
    public ReadResult get(boolean end, ISink sink) {

        ReadResult readResult = source.get(end, sink);
        if(ReadResultEnum.Available.equals(readResult.status)){

            String json = gson.toJson(readResult.data);
            byte[] tmp = json.getBytes(StandardCharsets.UTF_8);
            byteBuffer = ByteBuffer.allocate(tmp.length+8);
            byteBuffer.putInt(tmp.length+4);

            if( readResult.data instanceof Update){
                byteBuffer.putInt(1);
            }else if(readResult.data instanceof Outgoing) {
                byteBuffer.putInt(2);
            }else if(readResult.data instanceof String){
                byteBuffer.putInt(3);
            }else{
                throw new IllegalArgumentException("unsupported data type");
            }

            byteBuffer.put(tmp).flip();

            readResult.data = byteBuffer.array();
        }

        return readResult;
    }
}
