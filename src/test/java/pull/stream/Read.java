package pull.stream;


import static pull.stream.ReadResult.ReadResultEnum.*;

public class Read implements IRead {

    int n = 5;

    @Override
    public ReadResult invoke(boolean end, ISink sink) {

        if( n <= 0 ){
            return new ReadResult(End, -1);
        }

        return new ReadResult(Continue, n--);
    }

}
