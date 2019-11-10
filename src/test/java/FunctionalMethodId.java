import org.junit.Test;

import java.util.function.Consumer;

public class FunctionalMethodId {

    Consumer c = this::func;

    public void func(Object b){

    }

    @Test
    public void test(){
        FunctionalMethodId a = new FunctionalMethodId();
        Consumer f1 = b -> a.func(null);
        Consumer f2 = b -> a.func(null);

        System.out.println( c );
        System.out.println( (Consumer)this::func );
        System.out.println( (Consumer)this::func );


    }
}
