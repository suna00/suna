package net.ion.ice.core.infinispan;

/**
 * Created by jaeho on 2017. 6. 7..
 */
public class NotFoundNodeException extends RuntimeException {
    private String typeId ;
    private String id ;

    public NotFoundNodeException(String typeId, String id) {
        this.typeId = typeId ;
        this.id = id ;
    }
}
