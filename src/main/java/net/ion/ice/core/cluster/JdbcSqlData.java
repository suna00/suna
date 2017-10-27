package net.ion.ice.core.cluster;

import java.io.Serializable;

public class JdbcSqlData implements Serializable{
    private String sql ;
    private Object[] params ;

    public JdbcSqlData(String sql, Object... params){
        this.sql = sql ;
        this.params = params ;
    }

    public Object[] getParams() {
        return params;
    }

    public String getSql() {
        return sql;
    }
}
