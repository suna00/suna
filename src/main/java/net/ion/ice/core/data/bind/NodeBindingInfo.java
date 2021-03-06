package net.ion.ice.core.data.bind;

import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.context.Template;
import net.ion.ice.core.data.DBDataTypes;
import net.ion.ice.core.data.DBQuery;
import net.ion.ice.core.data.DBTypes;
import net.ion.ice.core.data.table.Column;
import net.ion.ice.core.infinispan.NotFoundNodeException;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryTerm;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by seonwoong on 2017. 6. 28..
 */

public class NodeBindingInfo implements Serializable {
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
    private String insertSequenceSql = "";
    private String updateSequenceSql = "";
    private String retrieveSequenceSql = "";

    private JdbcTemplate jdbcTemplate;


    private List<Column> columnList;
    private List<Column> createColumnList;
    private List<PropertyType> insertPids = new ArrayList<>();

    private List<PropertyType> updatePids = new ArrayList<>();
    private List<PropertyType> wherePids = new ArrayList<>();


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
                        insertPids.add(propertyType);

                        if (!propertyType.isIdable()) {
                            updateColumns.add(String.format("%s = ?", column.getColumnName()));
                            updatePids.add(propertyType);
                            break;
                        }
                        if (propertyType.isIdable()) {
                            whereIds.add(String.format("%s = ?", propertyType.getPid()));
                            wherePids.add(propertyType);
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
        updateSequenceSql = String.format("UPDATE datasequence SET sequence = sequence + 1 WHERE nodeType = '%s'", nodeType.getTypeId());
        insertSequenceSql = String.format("INSERT INTO datasequence (nodeType, sequence) VALUES ('%s', 1)", nodeType.getTypeId());
        retrieveSequenceSql = String.format("SELECT sequence FROM datasequence WHERE nodeType = '%s'", nodeType.getTypeId());
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
            throw new IceRuntimeException("db error ", e);
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
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(retrieveSql, parameters.toArray());
            return result ;
        }catch(EmptyResultDataAccessException e){
            logger.error("Node Binding Retrieve Error : "+ retrieveSql + " : " + id);
            throw new NotFoundNodeException("data", id) ;
        }
    }

    public Long retrieveSequence() {
        int callback = jdbcTemplate.update(updateSequenceSql);
        if (callback == 0) {
            jdbcTemplate.update(insertSequenceSql);
        }
        Long sequence = jdbcTemplate.queryForObject(retrieveSequenceSql, Long.class);

        return sequence;
    }


    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList(listSql);
    }

    public List<Map<String, Object>> list(QueryContext queryContext) {
        if (queryContext.getQueryTerms() != null && !queryContext.getQueryTerms().isEmpty()) {
            for (QueryTerm queryTerm : queryContext.getQueryTerms()) {
                if("EXISTS".equals(queryTerm.getMethod().toString())){
                    Template sqlTemplate = new Template(queryTerm.getQueryValue());
                    sqlTemplate.parsing();
                    Map<String, Object> result = jdbcTemplate.queryForMap(sqlTemplate.format(queryContext.getData()).toString(), sqlTemplate.getSqlParameterValues(queryContext.getData()));
                    queryTerm.setQueryValue(JsonUtils.getStringValue(result, "inValue"));
                }
            }
        }

        DBQuery dbQuery = new DBQuery(tableName, queryContext);
        queryContext.setDbQuery(dbQuery);
        Map<String, Object> totalCount;
        if (dbQuery.getResultCountValue() == null || dbQuery.getResultCountValue().isEmpty()) {
            totalCount = jdbcTemplate.queryForMap(dbQuery.getTotalCountSql());
        } else {
            totalCount = jdbcTemplate.queryForMap(dbQuery.getTotalCountSql(), dbQuery.getResultCountValue().toArray());
        }
        List<Map<String, Object>> items = jdbcTemplate.queryForList(dbQuery.getListParamSql(), dbQuery.getSearchListValue().toArray());
        queryContext.setResultSize(((Long) totalCount.get("totalCount")).intValue());

//        if(queryContext.getStart() > 0) {
//            items = items.subList(queryContext.getStart(), items.size()) ;
//        }

        queryContext.setQueryListSize(items.size());
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

    public int delete(String id) {
        List<String> parameters = retrieveParameters(id);
        int queryCallBack = jdbcTemplate.update(deleteSql, parameters.toArray());
        if(queryCallBack == 1){
            logger.info("Node Binding delete : "+ deleteSql + " : " + id);
        }
        return queryCallBack;
    }
    /**
     * 디비 저장 시 디비 컬럼이 null이 가능한지 체크 하고 가능하면 null 저장
     * 불가능하면 빈 텍스트 저장하도록 수정. 테스트 필요.. 문제되면 알려주세요.
     * */
    private List<Object> insertParameters(Map<String, String[]> parameterMap) {
        List<Object> insertParameters = new ArrayList<>();
        for (PropertyType pid : insertPids) {
            for (Column column : columnList) {
                if (column.getColumnName().equals(pid.getPid())) {
                    if (column.getNullable()) {
                        if (parameterMap.get(pid.getPid()) == null || parameterMap.get(pid.getPid())[0].equals("")) {
                            insertParameters.add(null);
                        } else {
                            insertParameters.add(parameterMap.get(pid.getPid())[0]);
                        }

                    } else {
                        if (parameterMap.get(pid.getPid()) == null || parameterMap.get(pid.getPid())[0].equals("")) {
                            insertParameters.add("");
                        } else {
                            insertParameters.add(parameterMap.get(pid.getPid())[0]);
                        }
                    }
                    break;
                }
            }
        }
        return insertParameters;
    }

    private List<Object> insertParameters(Node node) {
        List<Object> parameters = new ArrayList<>();
        for (PropertyType pid : insertPids) {
            parameters.add(extractNodeValue(node, pid.getPid()));
        }
        return parameters;
    }

    private List<Object> updateParameters(Map<String, String[]> parameterMap) {
        List<Object> updateParameters = new ArrayList<>();
        for (PropertyType pid : updatePids) {
            for (Column column : columnList) {
                if (column.getColumnName().equals(pid.getPid())) {
                    if (column.getNullable()) {
                        if (parameterMap.get(pid.getPid()) == null || parameterMap.get(pid.getPid())[0].equals("")) {
                            updateParameters.add(null);
                        } else {
                            updateParameters.add(parameterMap.get(pid.getPid())[0]);
                        }

                    } else {
                        if (parameterMap.get(pid.getPid()) == null || parameterMap.get(pid.getPid())[0].equals("")) {
                            updateParameters.add("");
                        } else {
                            updateParameters.add(parameterMap.get(pid.getPid())[0]);
                        }
                    }
                    break;
                }
            }
        }

        for (PropertyType pid : wherePids) {
            if (pid.getIdType().equals(PropertyType.IdType.autoIncrement)) {

            }
            updateParameters.add(parameterMap.get(pid.getPid())[0]);

        }
        return updateParameters;
    }

    private List<Object> updateParameters(Node node) {
        List<Object> parameters = new ArrayList<>();

        for (PropertyType pid : updatePids) {
            parameters.add(extractNodeValue(node, pid.getPid()));
        }

        for (PropertyType pid : wherePids) {
            parameters.add(extractNodeValue(node, pid.getPid()));
        }
        return parameters;
    }

    private List<Object> deleteParameters(Node node) {
        List<Object> parameters = new ArrayList<>();

        for (PropertyType pid : wherePids) {
            parameters.add(extractNodeValue(node, pid.getPid()));
        }
        return parameters;
    }


    public Object extractNodeValue(Node node, String pid) {
        return node.getBindingValue(pid);
    }

    private List<String> retrieveParameters(String id) {
        return Arrays.asList(id.split(Node.ID_SEPERATOR));
    }


    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}