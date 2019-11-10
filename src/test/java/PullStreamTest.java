import org.junit.Test;

import javax.xml.ws.Holder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class PullStreamTest {

    static int count = 0;

    static int n = 5;

    public void read(boolean end, BiConsumer<Boolean, Integer> cb){
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

    public void logger(BiConsumer<Boolean, BiConsumer<Boolean, Integer>> read){

        Holder<BiConsumer<Boolean, Integer>> holder = new Holder<>();
        holder.value  = (end, data) -> {
            if( end )   return;

            System.out.println(data);

            read.accept(false, holder.value);
        };

        read.accept(false, holder.value);
    }

    public BiConsumer<Boolean, BiConsumer<Boolean, Integer>> triple(
            BiConsumer<Boolean, BiConsumer<Boolean, Integer>> readable) {

        return (end, f) ->
            readable.accept(end, (isEnd, data) ->
                f.accept(isEnd, isEnd ? null :3 * data)
            );
    }

    public static class Pull{
        public static void pull(BiConsumer<Boolean, BiConsumer<Boolean, Integer>> source,
                         Function<BiConsumer<Boolean, BiConsumer<Boolean, Integer>>,
                                 BiConsumer<Boolean, BiConsumer<Boolean, Integer>>> through,
                         Consumer<BiConsumer<Boolean, BiConsumer<Boolean, Integer>>> sink
                         ){
            sink.accept(through.apply(source));
        }
    }



    @Test
    public void test(){
        logger( this.triple(this::read) );


        Pull.pull(this::read, this::triple, this::logger);

    }

    @Test
    public void jdkStream(){
        Stream.generate(()->new Random().nextInt())
                .forEach(System.out::println);

        int a = Stream.iterate(0, i -> i+1 )
                .filter( i -> i > 10 )
                .findAny()
                .get();

        System.out.println("a:"+a);
    }

    @Test
    public void testComparator(){
        Stream.of(1, 4, 2, 5, 3)
                .sorted(Comparator.comparingInt(a -> -a))
                .forEach(System.out::println);

        Integer[] array =Stream.of(1, 4, 2, 5, 3)
                .sorted((a,b)->b-a)
                .toArray(Integer[]::new);
        System.out.println(Arrays.toString(array));
    }

}
