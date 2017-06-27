package net.ion.ice.core.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by seonwoong on 2017. 6. 22..
 */

@Service("dataService")
public class DatabaseServiceIm implements DatabaseService {
    @Autowired
    private NodeService nodeService;
    @Autowired
    private DatabaseConfiguration configuration;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private Map<String, JdbcTemplate> dbJdbc;


    @ConfigurationProperties(prefix = "spring.datasource")
    @Bean
    public DataSource initJDBC() {
        return DataSourceBuilder
                .create()
                .build();
    }

    @Override
    public DataSource getDataSource(DatabaseConfiguration dataConfiguration) {
        if (StringUtils.equalsIgnoreCase("mysql", dataConfiguration.getJdbcType())) {
            return mySqlDataSource(dataConfiguration);
        } else if (StringUtils.equalsIgnoreCase("maria", dataConfiguration.getJdbcType())) {
            return mariaDataSource(dataConfiguration);
        } else { // oracle
            return oracleDataSource(dataConfiguration);
        }
    }

    @Override
    public void executeQuery(String dsId, String query, HttpServletResponse response) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            Node dataSourceNode = nodeService.read("datasource", dsId);

            configuration.setDbType(dataSourceNode.getStringValue("dbType"));
            configuration.setUsername(dataSourceNode.getStringValue("username"));
            configuration.setPassword(dataSourceNode.getStringValue("password"));
            configuration.setJdbcUrl(dataSourceNode.getStringValue("jdbcUrl"));

            jdbcTemplate = new JdbcTemplate(getDataSource(configuration));

            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);

            resultMap.put("items", results);
            resultMap.put("result", "200");
            resultMap.put("resultMessage", "success");
            mapper.writeValue(response.getWriter(), resultMap);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public BasicDataSource oracleDataSource(DatabaseConfiguration dataConfiguration) {
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

    public BasicDataSource mySqlDataSource(DatabaseConfiguration dataConfiguration) {
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

    public BasicDataSource mariaDataSource(DatabaseConfiguration dataConfiguration) {
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
