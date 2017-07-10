package net.ion.ice.core.data.bind;

import net.ion.ice.core.data.DBTypes;
import net.ion.ice.core.data.table.Column;
import net.ion.ice.core.data.table.TableMeta;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
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

    private String insertSql = "";
    private String updateSql = "";
    private String deleteSql = "";
    private String retieveSql = "";
    private JdbcTemplate jdbcTemplate;

    private List<Column> columnList;
    private List<Column> createColumnList;
    private List<String> insertPids = new ArrayList<>();

    private List<String> updatePids = new ArrayList<>();
    private List<String> wherePids = new ArrayList<>();

    private TableMeta tableMeta;

    public NodeBindingInfo(NodeType nodeType, JdbcTemplate jdbcTemplate, TableMeta tableMeta) {
        this.nodeType = nodeType;
        this.jdbcTemplate = jdbcTemplate;
        this.tableMeta = tableMeta;
    }

    public void init() {
        columnList = getTableColumns(tableMeta);

        createColumnList = new LinkedList<>();

        List<String> createColumns = new LinkedList<>();
        List<String> createPrimaryKeys = new LinkedList<>();

        List<String> updateColumns = new LinkedList<>();
        List<String> whereIds = new LinkedList<>();

        List<String> insertColumns = new LinkedList<>();
        List<String> insertSetKeys = new LinkedList<>();

        for (PropertyType propertyType : nodeType.getPropertyTypes()) {
            Column createColumn = new Column();
            createColumn.setColumnName(propertyType.getPid());
            createColumn.setDataType(tableMeta.getDBDataType(propertyType.getValueType()));

            if(propertyType.isIdable()){
                createColumn.setPk(true);
            }

            createColumn.setDefaultValue(propertyType.getDefaultValue());
            createColumn.setDataLength(propertyType.getLength());
            createColumn.setNullable(true);
            createColumnList.add(createColumn);

            if (columnList.size() > 0) {
                for (Column column : columnList) {
                    if (propertyType.getPid().equalsIgnoreCase(column.getColumnName())) {
                        insertColumns.add(column.getColumnName());
                        insertSetKeys.add("?");
                        insertPids.add(propertyType.getPid());

                        if (!propertyType.isIdable()) {
                            updateColumns.add(String.format("%s = ?", column.getColumnName()));
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

        }

        if (createColumnList.size() > 0) {
            for (Column column : createColumnList) {
                if (column.getPk()) {
                    createPrimaryKeys.add(String.format("%s", column.getColumnName()));
                }
                createColumns.add(String.format("%s %s(%s)", column.getColumnName(), column.getDataType(), column.getDataLength()));
            }
        }

        if (tableMeta.getDBType().equalsIgnoreCase("mySql") || tableMeta.getDBType().equalsIgnoreCase("maria")) {
            retieveSql = String.format("select * from %s with(nolock) where %s", tableMeta.getTableName(), StringUtils.join(whereIds.toArray(), " AND "));
        } else {
            retieveSql = String.format("select * from %s where %s", tableMeta.getTableName(), StringUtils.join(whereIds.toArray(), " AND "));
        }

        deleteSql = String.format("delete from %s where %s", tableMeta.getTableName(), StringUtils.join(whereIds.toArray(), " AND "));
        updateSql = String.format("UPDATE %s SET %s WHERE %s"
                , tableMeta.getTableName()
                , StringUtils.join(updateColumns.toArray(), ", ")
                , StringUtils.join(whereIds.toArray(), " AND "));

        insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)"
                , tableMeta.getTableName()
                , StringUtils.join(insertColumns.toArray(), ", ")
                , StringUtils.join(insertSetKeys.toArray(), ", "));

        if(createPrimaryKeys.size() > 0){
            createSql = String.format("CREATE TABLE %s (%s, CONSTRAINT %s PRIMARY KEY (%s))"
                    , tableMeta.getTableName()
                    , StringUtils.join(createColumns.toArray(), ", ")
                    , "PK_".concat(tableMeta.getTableName())
                    , StringUtils.join(createPrimaryKeys.toArray(), ", "));
        }else{
            createSql = String.format("CREATE TABLE %s (%s)"
                    , tableMeta.getTableName()
                    , StringUtils.join(createColumns.toArray(), ", "));
        }

    }

    public List<Column> getTableColumns(TableMeta tableMeta) {
        List<Column> columnList = new ArrayList<>();
        DatabaseMetaData dbMetaData;

        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            dbMetaData = connection.getMetaData();
            List<String> pkList = new ArrayList<>();
            String tableName = tableMeta.getTableName();

            if (tableMeta.getDBType().equals(DBTypes.oracle.name())) { //오라클은 대문자
                tableName = tableMeta.getTableName().toUpperCase();
            }

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

//    public List<Column> getCreateColumns() {
//        List<Column> columnList = new LinkedList<>();
//        for (PropertyType propertyType : nodeType.getPropertyTypes()) {
//            Column column = new Column();
//            column.setColumnName(propertyType.getPid());
//            if (DBType.equalsIgnoreCase(DBTypes.oracle.name())) {
//                column.setDataType(tableMetaSettings.setOracleDataTypes(propertyType.getValueType()));
//            } else {
//                column.setDataType(tableMetaSettings.setMysqlDataTypes(propertyType.getValueType()));
//            }
//            column.setDefaultValue(propertyType.getDefaultValue());
//            column.setDataLength(propertyType.getLength());
//            column.setNullable(true);
//            columnList.add(column);
//        }
//        return columnList;
//    }

    public void create() {
        jdbcTemplate.execute(createSql);
    }

    public int insert(Map<String, String[]> parameterMap) {
        List<Object> parameters = insertParameters(parameterMap);
        int queryCallBack = jdbcTemplate.update(insertSql, parameters.toArray());
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


    private List<Object> insertParameters(Map<String, String[]> parameterMap) {
        List<Object> parameters = new ArrayList<>();
        for (String pid : insertPids) {
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
}
