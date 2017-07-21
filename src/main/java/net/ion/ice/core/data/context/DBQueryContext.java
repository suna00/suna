package net.ion.ice.core.data.context;

import net.ion.ice.core.node.NodeType;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by seonwoong on 2017. 7. 20..
 */
public class DBQueryContext {
    protected NodeType nodeType;
    protected List<DBQueryTerm> dbQueryTermList;
    protected Map<String, Object> data;

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
            makeDBQueryTerm(dbQueryContext, dbQueryTermList, paramName, value);
        }
        return dbQueryContext;
    }

    public static void makeDBQueryTerm(DBQueryContext dbQueryContext, List<DBQueryTerm> dbQueryTermList, String paramName, String value) {
        value = value.equals("@sysdate") ? new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()) : value.equals("@sysday") ? new SimpleDateFormat("yyyyMMdd").format(new Date()) : value;
        DBQueryTerm dbQueryTerm =  new DBQueryTerm(paramName, value, "");

        dbQueryTermList.add(dbQueryTerm);
        dbQueryContext.setQueryTerms(dbQueryTermList);
    }

    public void setQueryTerms(List<DBQueryTerm> queryTerms) {
        this.dbQueryTermList = queryTerms;
    }

}
