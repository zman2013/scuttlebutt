package util;

import com.zman.scuttlebutt.AsyncScuttlebutt;
import com.zman.scuttlebutt.Scuttlebutt;
import com.zman.scuttlebutt.bean.Update;
import example.Model;
import org.junit.Test;

import java.util.Map;

public class RelationshipBetweenInstanceAndClass {

    @Test
    public void test(){
        Scuttlebutt asyncSB = new AsyncScuttlebutt("a") {
            @Override
            public <T> boolean applyUpdate(Update<T> update) {
                return false;
            }

            @Override
            public <T> Update<T>[] history(Map<String, Long> sources) {
                return new Update[0];
            }
        };

        Scuttlebutt sb = new Model("sb");

        System.out.println( AsyncScuttlebutt.class.isAssignableFrom(asyncSB.getClass()) );
        System.out.println( Scuttlebutt.class.isAssignableFrom(asyncSB.getClass()) );

        System.out.println( AsyncScuttlebutt.class.isAssignableFrom(sb.getClass()) );
        System.out.println( Scuttlebutt.class.isAssignableFrom(sb.getClass()) );
    }

}
