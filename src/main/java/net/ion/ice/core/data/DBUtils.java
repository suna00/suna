package net.ion.ice.core.data;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by seonwoong on 2017. 7. 7..
 */
public class DBUtils {

    private JdbcTemplate jdbcTemplate;

    public DBUtils(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
    }
    public String getDBType() {
        BasicDataSource basicDataSource = (BasicDataSource) jdbcTemplate.getDataSource();
        if (basicDataSource.getDriverClassName().equals(DBTypes.oracle.getDriverClassName())) {
            return "oracle";
        } else if (basicDataSource.getDriverClassName().equals(DBTypes.maria.getDriverClassName())) {
            return "maria";
        } else {
            return "mysql";
        }
    }
}
