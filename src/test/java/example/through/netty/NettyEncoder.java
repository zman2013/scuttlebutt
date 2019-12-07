package example.through.netty;

import com.zman.pull.stream.ISink;
import com.zman.pull.stream.bean.ReadResult;
import com.zman.pull.stream.bean.ReadResultEnum;
import example.through.Encoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NettyEncoder extends Encoder {

    @Override
    public ReadResult get(boolean end, ISink sink) {
        ReadResult readResult = super.get(end, sink);
        if(ReadResultEnum.Available.equals(readResult.status)){
            ByteBuf byteBuf = Unpooled.buffer(((byte[])readResult.data).length);
            byteBuf.writeBytes((byte[]) readResult.data);
            readResult.data = byteBuf;
        }

        return readResult;
    }
}
