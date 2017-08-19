package net.ion.ice;

import org.springframework.core.NestedRuntimeException;

/**
 * Created by jaeho on 2017. 5. 4..
 */
public class IceRuntimeException extends NestedRuntimeException {
    public IceRuntimeException(String msg) {
        super(msg);
    }

    public IceRuntimeException(String msg, Exception e) {
        super(msg, e);
    }
}
