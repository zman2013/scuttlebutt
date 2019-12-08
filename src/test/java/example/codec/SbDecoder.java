package example.codec;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zman.pull.stream.ISink;
import com.zman.pull.stream.bean.ReadResult;
import com.zman.pull.stream.bean.ReadResultEnum;
import com.zman.pull.stream.impl.DefaultThrough;
import com.zman.scuttlebutt.SbStream.Outgoing;
import com.zman.scuttlebutt.bean.Update;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class SbDecoder extends DefaultThrough<byte[], Object> {

    private Gson gson = new Gson();

    private Type updateDataType;

    public SbDecoder(Class updateDataType){
        this.updateDataType = TypeToken.getParameterized(Update.class, updateDataType).getType();
    }

    @Override
    public ReadResult get(boolean end, Throwable throwable, ISink sink) {
        ReadResult readResult = super.get(end, throwable, sink);

        if(ReadResultEnum.Available.equals(readResult.status)) {
            String data = new String((byte[]) readResult.data, StandardCharsets.UTF_8);
            if (data.startsWith("0")) {   // update
                readResult.data = gson.fromJson(data.substring(1), updateDataType);
            } else if (data.startsWith("1")) {
                readResult.data = gson.fromJson(data.substring(1), Outgoing.class);
            } else if (data.startsWith("2")) {
                readResult.data = data.substring(1);
            } else {
                throw new IllegalArgumentException();
            }
        }

        return readResult;
    }
}
