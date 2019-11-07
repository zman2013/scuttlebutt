import org.junit.Test;

import java.util.function.BiConsumer;

public class PullStreamTest {

    static int count = 0;

    static int n = 5;

    public static void read(boolean end, BiConsumer<Boolean, Integer> cb){
        if(end){
            cb.accept(end, null);
            return;
        }

        if( n-- < 0 ){
            cb.accept(true, null);
            return;
        }

        cb.accept(false, count++);
    }

    public static void logger(BiConsumer<Boolean, BiConsumer<Boolean, Integer>> read){

        Recursive<BiConsumer<Boolean, Integer>> recursive = new Recursive<>();
        recursive.func = (end, data) -> {
            if( end )   return;

            System.out.println(data);

            read.accept(false, recursive.func);
        };

        read.accept(false, recursive.func);
    }


    public static class Recursive<I> {
        public I func;
    }

    @Test
    public void test(){
        logger(PullStreamTest::read);
    }

}
