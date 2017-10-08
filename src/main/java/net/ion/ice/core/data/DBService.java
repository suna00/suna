package net.ion.ice.core.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by seonwoong on 2017. 6. 22..
 */

@Service("DBService")
public class DBService {
    private static Logger logger = LoggerFactory.getLogger(DBService.class);

    @Autowired
    private NodeService nodeService;

    public static Map<String, JdbcTemplate> dataSourceTemplate = new ConcurrentHashMap<>();

    public JdbcTemplate getJdbcTemplate(String dsId) {
        if (!dataSourceTemplate.containsKey(dsId)) {
//            Node dataSourceNode = nodeService.getDatasource(dsId); 캐시가 없을때, nodeService를 못가져옴
            Node dataSourceNode = NodeUtils.getNodeService().getDatasource(dsId);
            DBConfiguration configuration = new DBConfiguration(dataSourceNode);
            dataSourceTemplate.put(dsId, new JdbcTemplate(setDataSource(configuration)));
        }
        JdbcTemplate jdbcTemplate = dataSourceTemplate.get(dsId);
        return jdbcTemplate;
    }



    public void executeQuery(String dsId, String query, HttpServletResponse response) {
        Map<String, Object> resultMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Map<String, Object>> results = getJdbcTemplate(dsId).queryForList(query);

            resultMap.put("items", results);
            resultMap.put("result", "200");
            resultMap.put("resultMessage", "success");
            mapper.writeValue(response.getWriter(), resultMap);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static DataSource setDataSource(DBConfiguration dataConfiguration) {

        if (StringUtils.equalsIgnoreCase(dataConfiguration.getDbType(), "mySql")) {
            return mySqlDataSource(dataConfiguration);

        } else if (StringUtils.equalsIgnoreCase(dataConfiguration.getDbType(), "msSql")) {
            return msSqlDataSource(dataConfiguration);

        } else if (StringUtils.equalsIgnoreCase(dataConfiguration.getDbType(), "maria")) {
            return mariaDataSource(dataConfiguration);

        } else {
            return oracleDataSource(dataConfiguration);
        }
    }


    public static DataSource oracleDataSource(DBConfiguration dataConfiguration) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(DBTypes.oracle.getDriverClassName());
        basicDataSource.setUsername(dataConfiguration.getUsername());
        basicDataSource.setPassword(dataConfiguration.getPassword());
        basicDataSource.setUrl(dataConfiguration.getJdbcUrl());
        basicDataSource.setInitialSize(3);
        basicDataSource.setMaxTotal(256);
        basicDataSource.setDefaultAutoCommit(true);
        basicDataSource.setRemoveAbandonedOnBorrow(true);
        basicDataSource.setMaxWaitMillis(3000);
        basicDataSource.setTestWhileIdle(true);
        basicDataSource.setTestOnBorrow(false);
        basicDataSource.setTestOnReturn(false);
        basicDataSource.setNumTestsPerEvictionRun(3);
        basicDataSource.setMinEvictableIdleTimeMillis(-1);
        basicDataSource.setValidationQueryTimeout(7);
        basicDataSource.setTestWhileIdle(true);
        basicDataSource.setTimeBetweenEvictionRunsMillis(60000);

        return basicDataSource;
    }

    public static DataSource mySqlDataSource(DBConfiguration dataConfiguration) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(DBTypes.mySql.getDriverClassName());
        basicDataSource.setUsername(dataConfiguration.getUsername());
        basicDataSource.setPassword(dataConfiguration.getPassword());
        basicDataSource.setUrl(dataConfiguration.getJdbcUrl());
        if(dataConfiguration.isSsl()){
            basicDataSource.setConnectionProperties("useSSL=true");
        }else{
            basicDataSource.setConnectionProperties("useSSL=false");
        }
        basicDataSource.setInitialSize(3);
        basicDataSource.setMaxTotal(256);
        basicDataSource.setDefaultAutoCommit(true);
        basicDataSource.setRemoveAbandonedOnBorrow(true);
        basicDataSource.setMaxWaitMillis(3000);
        basicDataSource.setTestWhileIdle(true);
        basicDataSource.setTestOnBorrow(false);
        basicDataSource.setTestOnReturn(false);
        basicDataSource.setNumTestsPerEvictionRun(3);
        basicDataSource.setMinEvictableIdleTimeMillis(-1);
        basicDataSource.setValidationQueryTimeout(7);
        basicDataSource.setTestWhileIdle(true);
        basicDataSource.setTimeBetweenEvictionRunsMillis(60000);
        return basicDataSource;
    }

    public static DataSource mariaDataSource(DBConfiguration dataConfiguration) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(DBTypes.maria.getDriverClassName());
        basicDataSource.setUsername(dataConfiguration.getUsername());
        basicDataSource.setPassword(dataConfiguration.getPassword());
        basicDataSource.setUrl(dataConfiguration.getJdbcUrl());
        basicDataSource.setInitialSize(3);
        basicDataSource.setMaxTotal(256);
        basicDataSource.setDefaultAutoCommit(true);
        basicDataSource.setRemoveAbandonedOnBorrow(true);
        basicDataSource.setMaxWaitMillis(3000);
        basicDataSource.setTestWhileIdle(true);
        basicDataSource.setTestOnBorrow(false);
        basicDataSource.setTestOnReturn(false);
        basicDataSource.setNumTestsPerEvictionRun(3);
        basicDataSource.setMinEvictableIdleTimeMillis(-1);
        basicDataSource.setValidationQueryTimeout(7);
        basicDataSource.setTestWhileIdle(true);
        basicDataSource.setTimeBetweenEvictionRunsMillis(60000);
        return basicDataSource;
    }

    public static DataSource msSqlDataSource(DBConfiguration dataConfiguration) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(DBTypes.msSql.getDriverClassName());
        basicDataSource.setUsername(dataConfiguration.getUsername());
        basicDataSource.setPassword(dataConfiguration.getPassword());
        basicDataSource.setUrl(dataConfiguration.getJdbcUrl());
        basicDataSource.setInitialSize(3);
        basicDataSource.setMaxTotal(256);
        basicDataSource.setDefaultAutoCommit(true);
        basicDataSource.setRemoveAbandonedOnBorrow(true);
        basicDataSource.setMaxWaitMillis(3000);
        basicDataSource.setTestWhileIdle(true);
        basicDataSource.setTestOnBorrow(false);
        basicDataSource.setTestOnReturn(false);
        basicDataSource.setNumTestsPerEvictionRun(3);
        basicDataSource.setMinEvictableIdleTimeMillis(-1);
        basicDataSource.setValidationQueryTimeout(7);
        basicDataSource.setTestWhileIdle(true);
        basicDataSource.setTimeBetweenEvictionRunsMillis(60000);
        return basicDataSource;
    }
}
