package net.ion.ice.core.data.bind;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.PropertyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Created by seonwoong on 2017. 6. 28..
 */
public class NodeBindingFunction {

    private String createQuery;
    private String updateQuery;
    private String deleteQuery;
    private String retrieveQuery;

    private List<PropertyType> createPropertyTypes;
    private List<PropertyType> updatePropertyTypes;

    public void NodeBindingToDB(){

    }

    public int create(Node node) {
        String query = "";
        JdbcTemplate jdbcTemplate = new JdbcTemplate();


        return jdbcTemplate.update(query);
    }
}
