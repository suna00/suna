package net.ion.ice.core.data;

import lombok.Data;
import net.ion.ice.core.node.Node;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;

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

    public DBConfiguration(Node dataSourceNode) {
        this.dsId = dataSourceNode.getStringValue("id");
        this.dbType = dataSourceNode.getStringValue("dbType");
        this.username = dataSourceNode.getStringValue("username");
        this.password = dataSourceNode.getStringValue("password");
        this.jdbcUrl = dataSourceNode.getStringValue("jdbcUrl");
        this.ssl = dataSourceNode.getBooleanValue("ssl");
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
}
