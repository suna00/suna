package net.ion.ice.core.data;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Created by seonwoong on 2017. 6. 26..
 */
public interface DatabaseService {
    public DataSource getDataSource(DatabaseConfiguration dataConfiguration);
    public void executeQuery(String dsId, String query, HttpServletResponse response);
}
