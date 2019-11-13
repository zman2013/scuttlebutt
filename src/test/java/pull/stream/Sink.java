package pull.stream;

import org.junit.Test;

public class Sink implements ISink {

    @Override
    public void invoke(IRead read) {

        boolean stop = false;
        while(!stop) {
            ReadResult readResult = read.invoke(false, this);

            switch (readResult.end){
                case Continue:
                    System.out.println(readResult.data);
                    break;
                case Waiting:
                case End:
                    stop = true;
            }
        }

    }

    @Test
    public void test(){
        Read read = new Read();
        Sink sink = new Sink();

        sink.invoke(read);
    }

}
