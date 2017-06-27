package net.ion.ice.core.data;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

/**
 * Created by seonwoong on 2017. 6. 26..
 */

@Repository
public class DatabaseDao {
    private JdbcTemplate jdbcTemplate;

    public List getList(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate.queryForList("select * from query");
    }
}
