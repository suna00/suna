package net.ion.ice.core.data;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;

/**
 * Created by jaeho on 2017. 6. 22..
 */
@RestController
public class DatabaseController {

    private static Logger logger = LoggerFactory.getLogger(DatabaseController.class);

    @Autowired
    private NodeService nodeService ;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private DatabaseDao databaseDao;

    @RequestMapping(value = "/data/{dsId}", method = RequestMethod.GET)
    @ResponseBody
    public Object query(WebRequest request, HttpServletResponse response, @PathVariable String dsId, @RequestParam(value="query") String query) throws IOException {
        try{
//            Node dataSourceNode = nodeService.read("datasource", dsId);
            databaseService.executeQuery(dsId, query, response);
        }catch (Exception e){

        }
        return null;
    }

}
