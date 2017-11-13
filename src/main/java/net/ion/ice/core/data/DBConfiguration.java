package net.ion.ice.core.data;

import net.ion.ice.core.node.Node;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Created by seonwoong on 2017. 6. 22..
 */

public class DBConfiguration implements Serializable {
    private static final long serialVersionUID = 5018763493730125750L;

    private String dsId;
    private String dbType;
    private String jdbcUrl;
    private String username;
    private String password;
    private boolean ssl;
    private Integer initialSize = 50;
    private Integer maxTotal = 100;


    public DBConfiguration(Node dataSourceNode) {
        this.dsId = dataSourceNode.getStringValue("id");
        this.dbType = dataSourceNode.getStringValue("dbType");
        this.username = dataSourceNode.getStringValue("username");
        this.password = dataSourceNode.getStringValue("password");
        this.jdbcUrl = dataSourceNode.getStringValue("jdbcUrl");
        this.ssl = dataSourceNode.getBooleanValue("ssl");
        if (dataSourceNode.getIntValue("initialSize") != null) {
            initialSize = dataSourceNode.getIntValue("initialSize");
        }
        if (dataSourceNode.getIntValue("maxTotal") != null) {
            maxTotal = dataSourceNode.getIntValue("maxTotal");
        }

    }

    public String getDsId() {
        return dsId;
    }


    public String getDbType() {
        return StringUtils.defaultString(dbType, "oracle");
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public boolean isSsl() {
        return ssl;
    }

    public Integer getInitialSize() {
        return initialSize;
    }

    public Integer getMaxTotal() {
        return maxTotal;
    }

    @Override
    public String toString() {
        return "DBConfiguration{" +
                "dsId='" + dsId + '\'' +
                ", dbType='" + dbType + '\'' +
                ", jdbcUrl='" + jdbcUrl + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", ssl=" + ssl +
                ", initialSize=" + initialSize +
                ", maxTotal=" + maxTotal +
                '}';
    }
}
