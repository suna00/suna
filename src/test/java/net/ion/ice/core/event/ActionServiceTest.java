package net.ion.ice.core.event;

import net.ion.ice.core.context.Context;
import net.ion.ice.core.context.ExecuteContext;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by jaeho on 2017. 7. 12..
 */
public class ActionServiceTest {
    @Test
    public void execute() throws Exception {
        System.out.println(Context.class.isAssignableFrom(ExecuteContext.class));
        System.out.println(ExecuteContext.class.isAssignableFrom(Context.class));
    }

}