package com.zman.scuttlebutt.bean;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.TreeSet;

public class UpdateTest {

    @Test
    public void sort(){
        Update a = new Update(new Object[]{"a", 1}, System.currentTimeMillis(), "sourceId", "fromId");
        Update b = new Update(new Object[]{"a", 1}, System.currentTimeMillis()-1000, "sourceId", "fromId");
        Update c = new Update(new Object[]{"a", 1}, System.currentTimeMillis()+1000, "sourceId", "fromId");

        TreeSet<Update> set = new TreeSet<>();
        set.addAll(Arrays.asList(a,b,c));

        Assert.assertEquals(b, set.first());
        Assert.assertEquals(c, set.last());
    }

    @Test
    public void cloneTest(){
        long timestamp = System.currentTimeMillis();
        Update a = new Update(new Object[]{"a", 1}, timestamp, "sourceId", "fromId");
        Update clone = a.clone();

        Object[] data = (Object[]) clone.data;
        Assert.assertArrayEquals(new Object[]{"a",1}, data);
        Assert.assertEquals(timestamp, clone.timestamp);
        Assert.assertEquals("sourceId", clone.sourceId);
        Assert.assertEquals("fromId", clone.from);


    }

}
