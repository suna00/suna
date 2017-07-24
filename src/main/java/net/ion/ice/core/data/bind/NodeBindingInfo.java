package net.ion.ice.core.data.bind;

import net.ion.ice.core.data.DBDataTypes;
import net.ion.ice.core.data.DBTypes;
import net.ion.ice.core.data.context.DBQueryContext;
import net.ion.ice.core.data.context.DBQueryTerm;
import net.ion.ice.core.data.table.Column;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by seonwoong on 2017. 6. 28..
 */

public class NodeBindingInfo {
    private static Logger logger = LoggerFactory.getLogger(NodeBindingInfo.class);

    private NodeType nodeType;

    private String tableName = "";
    private String DBType = "";

    private String createSql = "";
    private String insertSql = "";
    private String updateSql = "";
    private String deleteSql = "";
    private String retrieveSql = "";
    private String listSql = "";
    private String listParamSql = "";

    private JdbcTemplate jdbcTemplate;


    private List<Column> columnList;
    private List<Column> createColumnList;
    private List<String> insertPids = new ArrayList<>();

    private List<String> updatePids = new ArrayList<>();
    private List<String> wherePids = new ArrayList<>();


    private Collection<PropertyType> propertyTypes;

    public NodeBindingInfo(NodeType nodeType, JdbcTemplate jdbcTemplate, String tableName, String DBType) {
        this.nodeType = nodeType;
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.DBType = DBType;
    }

    public void makeDefaultQuery() {
        columnList = getTableColumns(tableName, DBType);
        propertyTypes = nodeType.getPropertyTypes();
        List<String> createColumns = new LinkedList<>();
        List<String> createPrimaryKeys = new LinkedList<>();

        List<String> insertColumns = new LinkedList<>();
        List<String> insertSetKeys = new LinkedList<>();

        List<String> updateColumns = new LinkedList<>();
        List<String> whereIds = new LinkedList<>();

        createColumnList = new LinkedList<>();

        for (PropertyType propertyType : propertyTypes) {
            Column createColumn = new Column();
            createColumn.setColumnName(propertyType.getPid());
            createColumn.setDataType(DBDataTypes.convertDataType(DBType, propertyType.getValueType()));

            if (propertyType.isIdable()) {
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

        updatePids.addAll(wherePids);

        if (createColumnList.size() > 0) {
            for (Column column : createColumnList) {
                if (column.getPk()) {
                    createPrimaryKeys.add(String.format("%s", column.getColumnName()));
                }
                createColumns.add(String.format("%s %s(%s)", column.getColumnName(), column.getDataType(), column.getDataLength()));
            }
        }

        if (DBType.equalsIgnoreCase("mySql")) {
            listSql = String.format("SELECT * FROM %s LIMIT 1000", tableName);
            retrieveSql = String.format("SELECT * FROM %s WITH(nolock) WHERE %s", tableName, StringUtils.join(whereIds.toArray(), " AND "));

        } else if (DBType.equalsIgnoreCase("msSql")) {
            listSql = String.format("SELECT TOP 100 * FROM %s", tableName);
            retrieveSql = String.format("SELECT * FROM %s WHERE %s", tableName, StringUtils.join(whereIds.toArray(), " AND "));

        } else if (DBType.equalsIgnoreCase("maria")) {
            listSql = String.format("SELECT * FROM %s LIMIT 1000", tableName);
            retrieveSql = String.format("SELECT * FROM %s WHERE %s", tableName, StringUtils.join(whereIds.toArray(), " AND "));

        } else {
            listSql = String.format("SELECT * FROM %s WHERE ROWNUM <= 1000", tableName);
            retrieveSql = String.format("SELECT * FROM %s WHERE %s", tableName, StringUtils.join(whereIds.toArray(), " AND "));
        }

        deleteSql = String.format("delete from %s where %s", tableName, StringUtils.join(whereIds.toArray(), " AND "));
        updateSql = String.format("UPDATE %s SET %s WHERE %s"
                , tableName
                , StringUtils.join(updateColumns.toArray(), ", ")
                , StringUtils.join(whereIds.toArray(), " AND "));

        insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)"
                , tableName
                , StringUtils.join(insertColumns.toArray(), ", ")
                , StringUtils.join(insertSetKeys.toArray(), ", "));

        if (createPrimaryKeys.size() > 0) {
            createSql = String.format("CREATE TABLE %s (%s, CONSTRAINT %s PRIMARY KEY (%s))"
                    , tableName
                    , StringUtils.join(createColumns.toArray(), ", ")
                    , "PK_".concat(tableName)
                    , StringUtils.join(createPrimaryKeys.toArray(), ", "));
        } else {
            createSql = String.format("CREATE TABLE %s (%s)"
                    , tableName
                    , StringUtils.join(createColumns.toArray(), ", "));
        }

    }

    public List<Column> getTableColumns(String tableName, String DBType) {
        List<Column> columnList = new ArrayList<>();
        DatabaseMetaData dbMetaData;

        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            dbMetaData = connection.getMetaData();
            List<String> pkList = new ArrayList<>();

            if (DBType.equals(DBTypes.oracle.name())) { //오라클은 대문자
                tableName = tableName.toUpperCase();
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

    public void create() {
        jdbcTemplate.execute(createSql);
    }

    public int insert(Map<String, String[]> parameterMap) {
        List<Object> parameters = insertParameters(parameterMap);
        int queryCallBack = jdbcTemplate.update(insertSql, parameters.toArray());
        return queryCallBack;
    }

    public int insert(Node node) {
        List<Object> parameters = insertParameters(node);
        int queryCallBack = jdbcTemplate.update(insertSql, parameters.toArray());
        return queryCallBack;
    }

    public int update(Map<String, String[]> parameterMap) {
        List<Object> parameters = updateParameters(parameterMap);
        int queryCallBack = jdbcTemplate.update(updateSql, parameters.toArray());
        return queryCallBack;
    }

    public int update(Node node) {
        List<Object> parameters = updateParameters(node);
        int queryCallBack = jdbcTemplate.update(updateSql, parameters.toArray());
        return queryCallBack;
    }

    public Map<String, Object> retrieve(String id) {
        List<String> parameters = retrieveParameters(id);
        Map<String, Object> result = jdbcTemplate.queryForMap(retrieveSql, parameters.toArray());
        return result;
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = jdbcTemplate.queryForList(listSql);
        return result;
    }

    public List<Map<String, Object>> list(DBQueryContext dbQueryContext) {
        List<String> parameters = makeListQuery(dbQueryContext);
        List<Map<String, Object>> result = jdbcTemplate.queryForList(listParamSql, parameters.toArray());
        return result;
    }

    public int delete(Map<String, String[]> parameterMap) {
        List<Object> parameters = updateParameters(parameterMap);
        int queryCallBack = jdbcTemplate.update(deleteSql, parameters.toArray());
        return queryCallBack;
    }

    public int delete(Node node) {
        List<Object> parameters = updateParameters(node);
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

    private List<Object> insertParameters(Node Node) {
        List<Object> parameters = new ArrayList<>();
        for (String pid : insertPids) {
            parameters.add(Node.getValue(pid));
        }
        return parameters;
    }

    private List<Object> updateParameters(Map<String, String[]> parameterMap) {
        List<Object> parameters = new ArrayList<>();
        for (String pid : updatePids) {
            parameters.add(parameterMap.get(pid)[0]);
        }
        return parameters;
    }

    private List<Object> updateParameters(Node node) {
        List<Object> parameters = new ArrayList<>();
        for (String pid : updatePids) {
            parameters.add(node.getValue(pid));
        }
        return parameters;
    }

    private List<String> retrieveParameters(String id) {
        return Arrays.asList(id.split("@"));
    }

    public List makeListQuery(DBQueryContext dbQueryContext) {
        List<String> attachQuery = new ArrayList<>();
        List<String> attachValue = new ArrayList<>();
        if (!dbQueryContext.getDbQueryTermList().isEmpty()) {
            for (DBQueryTerm dbQueryTerm : dbQueryContext.getDbQueryTermList()) {
                String query = String.format("%s %s ?", dbQueryTerm.getKey(), dbQueryTerm.getMethodQuery());
                String value = dbQueryTerm.getValue();
                attachQuery.add(query);
                attachValue.add(value);
            }

            listParamSql = String.format("SELECT * FROM %s WHERE %s", tableName, StringUtils.join(attachQuery.toArray(), " AND "));

        } else {

            listParamSql = String.format("SELECT * FROM %s WHERE %s", tableName, StringUtils.join(attachQuery.toArray(), " AND "));
        }


        return attachValue;
    }
}