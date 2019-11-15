import com.zman.scuttlebutt.AsyncScuttlebutt;
import com.zman.scuttlebutt.Scuttlebutt;
import com.zman.scuttlebutt.basic.Model;
import org.junit.Test;

import java.util.Map;

public class RelationshipBetweenInstanceAndClass {

    @Test
    public void test(){
        Scuttlebutt asyncSB = new AsyncScuttlebutt("a") {
            @Override
            public boolean applyUpdate(Update update) {
                return false;
            }

            @Override
            public Update[] history(Map<String, Long> sources) {
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
