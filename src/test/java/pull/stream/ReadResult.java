package pull.stream;


public class ReadResult {

    public ReadResultEnum end;

    public Object data;

    public ReadResult(ReadResultEnum remain, int n) {
        this.end = remain;
        this.data = n;
    }

    enum ReadResultEnum{
        Continue,
        Waiting,
        End
    }
}
