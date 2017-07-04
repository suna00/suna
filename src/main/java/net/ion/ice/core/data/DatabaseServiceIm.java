package net.ion.ice.core.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by seonwoong on 2017. 6. 22..
 */

@Service("dataService")
public class DatabaseServiceIm implements DatabaseService {
    @Autowired
    private NodeService nodeService;

    @Autowired
    ObjectMapper mapper;

    static Map<String, JdbcTemplate> dataSourceTemplate = new ConcurrentHashMap<>();

    @PostConstruct
    public void initJdbcDataSource() {
//        for(Node dataSourceNode : nodeService.getNodeList("datasource", "")){
//            setDatabaseConfiguration(dataSourceNode);
//            dataSourceTemplate.put((String) dataSourceNode.get("id"), new JdbcTemplate(setDataSource(configuration)));
//        }
    }

    public JdbcTemplate getJdbcTemplate(String dsId) {
        if (!dataSourceTemplate.containsKey(dsId)) {
            Node dataSourceNode = nodeService.read("datasource", dsId);
            DatabaseConfiguration configuration =  new DatabaseConfiguration(dataSourceNode) ;
            dataSourceTemplate.put(dsId, new JdbcTemplate(setDataSource(configuration)));
        }
        JdbcTemplate jdbcTemplate = dataSourceTemplate.get(dsId);
        return jdbcTemplate;
    }

    public static DataSource setDataSource(DatabaseConfiguration dataConfiguration) {

        if (StringUtils.equalsIgnoreCase(dataConfiguration.getDbType(), "mysql")) {
            return mySqlDataSource(dataConfiguration);

        } else if (StringUtils.equalsIgnoreCase(dataConfiguration.getDbType(), "mariadb")) {
            return mariaDataSource(dataConfiguration);

        } else {
            return oracleDataSource(dataConfiguration);
        }
    }

    public void createDatabaseConfiguration(Node dataSourceNode) {

    }

    @Override
    public void executeQuery(String dsId, String query, HttpServletResponse response) {
        Map<String, Object> resultMap = new HashMap<>();

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

    public static DataSource oracleDataSource(DatabaseConfiguration dataConfiguration) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName("oracle.jdbc.OracleDriver");
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

    public static DataSource mySqlDataSource(DatabaseConfiguration dataConfiguration) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName("com.mysql.jdbc.Driver");
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

    public static DataSource mariaDataSource(DatabaseConfiguration dataConfiguration) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName("org.mariadb.jdbc.Driver");
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
