package net.ion.ice.core.node;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by jaeho on 2017. 6. 7..
 */
public class NodeHistory implements Serializable {
    private Integer version ;
    private Date date ;
    private String userId ;
    private String execute ;
    private String referer ;

    private Map<String, Object> properties ;


}
