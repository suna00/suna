package net.ion.ice.core.data.bind;

import net.ion.ice.core.data.DBType;
import net.ion.ice.core.data.table.Column;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by seonwoong on 2017. 6. 28..
 */

public class NodeBindingInfo {

    private NodeType nodeType;
    private String createSql = "";
    private String updateSql = "";
    private String deleteSql = "";
    private String retieveSql = "";

    private JdbcTemplate jdbcTemplate;
    private List<Column> columnList;

    private List<String> createPids = new ArrayList<>();
    private List<String> updatePids = new ArrayList<>();
    private List<String> wherePids = new ArrayList<>();

    public NodeBindingInfo(NodeType nodeType, JdbcTemplate jdbcTemplate) {
        this.nodeType = nodeType;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void init() {
        String tableName = String.valueOf(nodeType.getTableName()).split("#")[1];
        String dbType = getDBType(jdbcTemplate);

        columnList = getTableColumns(tableName);
        List<String> updateColumns = new LinkedList<>();
        List<String> whereIds = new LinkedList<>();

        List<String> createColumns = new LinkedList<>();
        List<String> createSetKeys = new LinkedList<>();
//        querySetProperties = new LinkedList<>();


        for (PropertyType propertyType : nodeType.getPropertyTypes()) {
            for (Column column : columnList) {
                if (propertyType.getPid().equalsIgnoreCase(column.getColumnName())) {
                    createColumns.add(propertyType.getPid());
                    createSetKeys.add("?");
                    createPids.add(propertyType.getPid());

                    if (!propertyType.isIdable()) {
                        updateColumns.add(String.format("%s = ?", propertyType.getPid()));
                        updatePids.add(propertyType.getPid());
                        break;
                    }
                    if (propertyType.isIdable()) {
                        whereIds.add(String.format("%s = ?", propertyType.getPid()));
                        wherePids.add(propertyType.getPid());
                        break;
                    }
                }
            }
        }

        if (dbType.equalsIgnoreCase("mysql")) {
            retieveSql = String.format("select * from %s with(nolock) where %s", tableName, StringUtils.join(whereIds.toArray(), " AND "));
        } else {
            retieveSql = String.format("select * from %s where %s", tableName, StringUtils.join(whereIds.toArray(), " AND "));
        }

        deleteSql = String.format("delete from %s where %s", tableName, StringUtils.join(whereIds.toArray(), " AND "));
        updateSql = String.format("UPDATE %s SET %s WHERE %s"
                , tableName
                , StringUtils.join(updateColumns.toArray(), ", ")
                , StringUtils.join(whereIds.toArray(), " AND "));

        createSql = String.format("INSERT INTO %s (%s) VALUES (%s)"
                , tableName
                , StringUtils.join(createColumns.toArray(), ", ")
                , StringUtils.join(createSetKeys.toArray(), ", "));
    }

    public List<Column> getTableColumns(String tableName) {
        List<Column> columnList = new ArrayList<>();
        DatabaseMetaData dbMetaData;
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            dbMetaData = connection.getMetaData();
            List<String> pkList = new ArrayList<>();
            try (ResultSet rs = dbMetaData.getPrimaryKeys(null, null, tableName)) {
                while (rs.next()) {
                    pkList.add(rs.getString("COLUMN_NAME"));
                }
            }

            try (ResultSet columnsRs = dbMetaData.getColumns(null, null, tableName, null)) {
                while (columnsRs.next()) {
                    String columnName = columnsRs.getString("COLUMN_NAME");
                    String dataType = columnsRs.getString("TYPE_NAME");
                    Integer dataLength = columnsRs.getInt("COLUMN_SIZE");
                    Boolean isPk = pkList.contains(columnName);
                    Boolean isNullable = columnsRs.getInt("NULLABLE") == 0 ? false : true;
                    Integer sqlType = columnsRs.getInt("DATA_TYPE");
                    String dataDefault = columnsRs.getString("COLUMN_DEF");

                    Column column = new Column(columnName, isPk, dataType, dataLength, isNullable, sqlType, dataDefault);
                    columnList.add(column);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return columnList;
    }

    public int create(Map<String, String[]> parameterMap) {
        List<Object> parameters = createParameters(parameterMap);
        int queryCallBack = jdbcTemplate.update(createSql, parameters.toArray());
        return queryCallBack;
    }

    public int update(Map<String, String[]> parameterMap) {
        List<Object> parameters = updateParameters(parameterMap);
        int queryCallBack = jdbcTemplate.update(updateSql, parameters.toArray());
        return queryCallBack;
    }

//    public int retrieve(Map<String, String[]> parameterMap) {
//        List<Object> parameters = updateParameters(parameterMap);
//        int queryCallBack = jdbcTemplate.query(retieveSql);
//        return queryCallBack;
//    }

    public int delete(Map<String, String[]> parameterMap) {
        List<Object> parameters = updateParameters(parameterMap);
        int queryCallBack = jdbcTemplate.update(deleteSql, parameters.toArray());
        return queryCallBack;
    }

    private List<Object> createParameters(Map<String, String[]> parameterMap) {
        List<Object> parameters = new ArrayList<>();
        for (String pid : createPids) {
            parameters.add(parameterMap.get(pid)[0]);
        }
        return parameters;
    }

    private List<Object> updateParameters(Map<String, String[]> parameterMap) {
        List<Object> parameters = new ArrayList<>();
        updatePids.addAll(wherePids);
        for (String pid : updatePids) {
            parameters.add(parameterMap.get(pid)[0]);
        }
        return parameters;
    }

    private String getDBType(JdbcTemplate jdbcTemplate) {
        BasicDataSource basicDataSource = (BasicDataSource) jdbcTemplate.getDataSource();
        if (basicDataSource.getDriverClassName().equals(DBType.ORACLE)) {
            return "oracle";
        } else if (basicDataSource.getDriverClassName().equals(DBType.MARIA)) {
            return "maria";
        } else {
            return "mysql";
        }
    }
}
