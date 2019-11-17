package util;

import org.junit.Assert;
import org.junit.Test;

/**
 * 每次调用this::func都会创建不同的函数
 */
public class FunctionalMethodId {

    Runnable c = this::func;

    public void func(){}

    @Test
    public void test(){
        Assert.assertNotEquals(c, (Runnable)this::func);
        Assert.assertNotEquals((Runnable)this::func, (Runnable)this::func);
    }
}
