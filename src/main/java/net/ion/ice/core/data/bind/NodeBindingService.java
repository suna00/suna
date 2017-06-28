package net.ion.ice.core.data.bind;

import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by seonwoong on 2017. 6. 28..
 */
@Service
public class NodeBindingService {

    List<PropertyType> propertyTypes;
    public void saveNode(Map<String, Object> data) {
        String tid = (String) data.get("tid");

    }
}
