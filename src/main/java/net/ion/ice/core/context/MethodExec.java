package net.ion.ice.core.context;

import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 23..
 */
public interface MethodExec {

    String execute(String[] methodParams, Object value, Map<String, Object> data) ;
}
