//package net.ion.ice.core.data;
//
//import net.ion.ice.core.node.Node;
//import net.ion.ice.core.node.NodeController;
//import net.ion.ice.core.node.NodeService;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.context.request.WebRequest;
//
//import javax.sql.DataSource;
//import java.io.IOException;
//
///**
// * Created by jaeho on 2017. 6. 22..
// */
//public class DataController {
//
//    private static Logger logger = LoggerFactory.getLogger(DataController.class);
//
//    @Autowired
//    private NodeService nodeService ;
//
//    @RequestMapping(value = "/data/{dsId}", method = RequestMethod.GET)
//    @ResponseBody
//    public Object query(WebRequest request, @PathVariable String dsId, @RequestParam(value="query") String query) throws IOException {
//
//        Node dataSourceNode = nodeService.read("datasource", dsId);
//        JDBC  getDbDataSource(dataSourceNode) ;
//
//        return save(request, typeId);
//    }
//
//}
