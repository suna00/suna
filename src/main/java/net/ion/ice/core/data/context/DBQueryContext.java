package net.ion.ice.core.data.context;

import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by seonwoong on 2017. 7. 20..
 */
public class DBQueryContext {
    protected NodeType nodeType;
    protected List<DBQueryTerm> dbQueryTermList;
    protected Map<String, Object> data;
    protected String sorting;
    protected Integer pageSize;
    protected Integer currentPage;
    protected Integer maxSize;
    protected boolean paging;

    public DBQueryContext(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public static DBQueryContext makeDBQueryContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        DBQueryContext dbQueryContext = new DBQueryContext(nodeType);
        List<DBQueryTerm> dbQueryTermList = new ArrayList<>();
        if (parameterMap == null || parameterMap.size() == 0) {
            return dbQueryContext;
        }

        for (String paramName : parameterMap.keySet()) {
            String[] values = parameterMap.get(paramName);
            if (values == null || StringUtils.isEmpty(values[0])) {
                continue;
            }
            String value = StringUtils.join(values, ' ');
            makeDBQueryTerm(nodeType, dbQueryContext, dbQueryTermList, paramName, value);
        }

        dbQueryContext.setDbQueryTermList(dbQueryTermList);
        return dbQueryContext;
    }

    public static void makeDBQueryTerm(NodeType nodeType, DBQueryContext dbQueryContext, List<DBQueryTerm> dbQueryTermList, String paramName, String value) {
//        value = value.equals("@sysdate") ? new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()) : value.equals("@sysday") ? new SimpleDateFormat("yyyyMMdd").format(new Date()) : value;

        if (paramName.equals("page")) {
            dbQueryContext.setCurrentPage(value);
            return;
        } else if (paramName.equals("pageSize")) {
            dbQueryContext.setPageSize(value);
            return;
        } else if (paramName.equals("count")) {
            dbQueryContext.setMaxSize(value);
            return;
        } else if (paramName.equals("sorting")) {
            dbQueryContext.setSorting(value);
            return;
        }
        String propertyType = StringUtils.substringBeforeLast(paramName, "_");
        String method = StringUtils.substringAfterLast(paramName, "_");

        DBQueryTerm dbQueryTerm;

        if (method.equals("")) {
            dbQueryTerm = makePropertyQueryTerm(nodeType, propertyType, "equals", value);
        } else if (method.equals("matching")) {
            dbQueryTerm = makePropertyQueryTerm(nodeType, propertyType, method, "%".concat(value).concat("%"));
        } else {
            dbQueryTerm = makePropertyQueryTerm(nodeType, propertyType, method, value);
        }

        if (dbQueryTerm == null) {
            dbQueryTerm = makePropertyQueryTerm(nodeType, paramName, "matching", "%".concat(value).concat("%"));
        }

        dbQueryTermList.add(dbQueryTerm);
    }

    public static DBQueryTerm makePropertyQueryTerm(NodeType nodeType, String fieldId, String method, String value) {
        PropertyType propertyType = nodeType.getPropertyType(fieldId);
        if (propertyType != null && propertyType.isIndexable()) {
            return new DBQueryTerm(fieldId, method, value);
        }
        return null;
    }

    public void setDbQueryTermList(List<DBQueryTerm> dbQueryTermList) {
        this.dbQueryTermList = dbQueryTermList;
    }

    public List<DBQueryTerm> getDbQueryTermList() {
        return dbQueryTermList;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = Integer.valueOf(pageSize);
        this.paging = true;
    }

    public Integer getPageSize() {
        return pageSize == null ? 1000 : pageSize;
    }

    public void setCurrentPage(String page) {
        this.currentPage = Integer.valueOf(page);
        this.paging = true;
    }

    public Integer getCurrentPage() {
        return currentPage == null ? 1 : currentPage;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = Integer.valueOf(maxSize);
    }

    public void setSorting(String sortingStr) {
        this.sorting = sortingStr;
    }

    public String getSorting() {
        return sorting;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

}
