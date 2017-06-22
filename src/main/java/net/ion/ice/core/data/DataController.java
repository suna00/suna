package net.ion.ice.core.data;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeController;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Created by jaeho on 2017. 6. 22..
 */
public class DataController {

    private static Logger logger = LoggerFactory.getLogger(DataController.class);

    @Autowired
    private NodeService nodeService ;

//    @RequestMapping(value = "/data/{dsId}", method = RequestMethod.GET)
//    @ResponseBody
//    public Object query(WebRequest request, @PathVariable String dsId, @RequestParam(value="query") String query) throws IOException {
//
//        Node dataSourceNode = nodeService.read("datasource", dsId);
//        JDbcTemplate  getDbDataSource(dataSourceNode) ;
//
//        return save(request, typeId);
//    }
//
//    public static DataSource getDbDataSource(LogicConfig logicServer){
//        BasicDataSource dataSource = new BasicDataSource();
//        if(StringUtils.equals(logicServer.getJdbcType(), "oracle")){
//            dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
//            dataSource.setValidationQuery("select 1 from dual");
//        }else if(StringUtils.equals(logicServer.getJdbcType(), "mysql")){
//            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
//            dataSource.setValidationQuery("select 1");
//        }else if(StringUtils.equals(logicServer.getJdbcType(), "maria")){
//            dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
//            dataSource.setValidationQuery("select 1");
//        }
//        dataSource.setUrl(logicServer.getUrl());
//        dataSource.setUsername(logicServer.getUserNm());
//        dataSource.setPassword(logicServer.getPwd());
//        dataSource.setInitialSize(3);
//        dataSource.setMaxActive(256);
//        dataSource.setDefaultAutoCommit(true);
//        dataSource.setRemoveAbandoned(true);
//        dataSource.setMaxWait(3000);
//        dataSource.setTestWhileIdle(true);
//        dataSource.setTestOnBorrow(false);
//        dataSource.setTestOnReturn(false);
//        dataSource.setNumTestsPerEvictionRun(3);
//        dataSource.setMinEvictableIdleTimeMillis(-1);
//        dataSource.setValidationQueryTimeout(7);
//        dataSource.setTestWhileIdle(true);
//        dataSource.setTimeBetweenEvictionRunsMillis(60000);
//        return dataSource;
//    }
}
