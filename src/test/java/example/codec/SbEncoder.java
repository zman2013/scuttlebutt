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

public class SbEncoder extends DefaultThrough<Object, byte[]> {

    private Type dataType;

    private Gson gson = new Gson();

    public SbEncoder(Class updateDataType){
        dataType = TypeToken.getParameterized(Update.class, updateDataType).getType();
    }

    @Override
    public ReadResult get(boolean end, Throwable throwable, ISink sink) {
        ReadResult readResult = super.get(end, throwable, sink);

        if(ReadResultEnum.Available.equals(readResult.status)){
            Object data = readResult.data;
            if(data instanceof Update){
                readResult.data = ("0"+gson.toJson(data, dataType)).getBytes(StandardCharsets.UTF_8);
            }else if(data instanceof Outgoing){
                readResult.data = ("1"+gson.toJson(data)).getBytes(StandardCharsets.UTF_8);
            }else if(data instanceof String){
                readResult.data = ("2"+data).getBytes(StandardCharsets.UTF_8);
            }else{
                readResult = new ReadResult(ReadResultEnum.Closed, new IllegalArgumentException("unknown data type"));
            }
        }

        return readResult;
    }
}
