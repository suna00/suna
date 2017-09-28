package net.ion.ice.core.file;

import org.springframework.core.NestedRuntimeException;

/**
 * Created by juneyoungoh on 2017. 9. 28..
 */
public class TolerableMissingFileException extends NestedRuntimeException {
    public TolerableMissingFileException(String msg) {
        super(msg);
    }
    public TolerableMissingFileException(String msg, Exception e) {
        super(msg, e);
    }
}
