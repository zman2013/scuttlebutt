package pull.stream;

public interface IRead {

    ReadResult invoke(boolean end, ISink sink);

}
