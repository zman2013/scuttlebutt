package pull.stream;

public interface ISink {

    void invoke(IRead read);

}
