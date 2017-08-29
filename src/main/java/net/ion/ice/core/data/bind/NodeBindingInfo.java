package net.ion.ice.core.data.bind;

import net.ion.ice.core.context.DataQueryContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.data.DBDataTypes;
import net.ion.ice.core.data.DBTypes;
import net.ion.ice.core.context.DBQueryContext;
import net.ion.ice.core.context.DBQueryTerm;
import net.ion.ice.core.data.table.Column;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryTerm;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private String totalCountSql = "";


    private List<String> searchListQuery;
    private List<Object> searchListValue;
    private List<Object> resultCountValue;


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
            retrieveSql = String.format("SELECT * FROM %s WHERE %s", tableName, StringUtils.join(whereIds.toArray(), " AND "));

        } else if (DBType.equalsIgnoreCase("msSql")) {
            listSql = String.format("SELECT TOP 1000 * FROM %s", tableName);
            retrieveSql = String.format("SELECT * FROM %s WITH(nolock) WHERE %s", tableName, StringUtils.join(whereIds.toArray(), " AND "));

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


        logger.info("================================== :: " + this.createSql);
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
        return jdbcTemplate.queryForList(listSql);
    }

    public List<Map<String, Object>> list(QueryContext queryContext) {
        makeListQuery(queryContext);
        Map<String, Object> totalCount;
        if (resultCountValue == null || resultCountValue.isEmpty()) {
            totalCount = jdbcTemplate.queryForMap(totalCountSql);
        } else {
            totalCount = jdbcTemplate.queryForMap(totalCountSql, resultCountValue.toArray());
        }
        List<Map<String, Object>> items = jdbcTemplate.queryForList(listParamSql, searchListValue.toArray());
        queryContext.setResultSize(((Long) totalCount.get("totalCount")).intValue());
        queryContext.setQueryListSize(items.size()) ;
        return items;
    }

    public int delete(Map<String, String[]> parameterMap) {
        List<Object> parameters = updateParameters(parameterMap);
        int queryCallBack = jdbcTemplate.update(deleteSql, parameters.toArray());
        return queryCallBack;
    }

    public int delete(Node node) {
        List<Object> parameters = deleteParameters(node);
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

    private List<Object> insertParameters(Node node) {
        List<Object> parameters = new ArrayList<>();
        for (String pid : insertPids) {
            parameters.add(extractNodeValue(node, pid));
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
            parameters.add(extractNodeValue(node, pid));
        }
        return parameters;
    }

    private List<Object> deleteParameters(Node node) {
        List<Object> parameters = new ArrayList<>();

        for (String pid : wherePids) {
            parameters.add(extractNodeValue(node, pid));
        }
        return parameters;
    }

    public Object extractNodeValue(Node node, String pid) {
        return node.getBindingValue(pid);
    }

    private List<String> retrieveParameters(String id) {
        return Arrays.asList(id.split(Node.ID_SEPERATOR));
    }

    public void makeListQuery(QueryContext queryContext) {
        searchListQuery = new ArrayList<>();
        searchListValue = new ArrayList<>();

        String sorting = queryContext.getSorting();

        if (queryContext.getQueryTerms() != null &&!queryContext.getQueryTerms().isEmpty()) {
            for (QueryTerm queryTerm : queryContext.getQueryTerms()) {
                String query = String.format("%s %s ?", queryTerm.getQueryKey(), queryTerm.getMethodQuery());
                String value = queryTerm.getQueryValue();
                searchListQuery.add(query);
                searchListValue.add(value);
            }

            listParamSql = String.format("SELECT * FROM %s WHERE %s", tableName, StringUtils.join(searchListQuery.toArray(), " AND "));
            totalCountSql = String.format("SELECT COUNT(*) as totalCount FROM %s WHERE %s", tableName, StringUtils.join(searchListQuery.toArray(), " AND "));

            resultCountValue = new ArrayList<>(searchListValue);

        } else {
            listParamSql = String.format("SELECT * FROM %s", tableName);
            totalCountSql = String.format("SELECT COUNT(*) as totalCount FROM %s", tableName);

            resultCountValue = new ArrayList<>();
        }

        if (sorting != null) {
            listParamSql = listParamSql.concat(String.format(" ORDER BY ").concat(sorting));
        }

        listParamSql = listParamSql.concat(String.format(" LIMIT ?").concat(String.format(" OFFSET ?")));
        searchListValue.add(queryContext.getLimit());
        searchListValue.add(queryContext.getOffset());
    }
}