package net.ion.ice.core.data;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * Created by seonwoong on 2017. 6. 22..
 */

@Data
@Configuration
public class DatabaseConfiguration implements Serializable {
    private static final long serialVersionUID = 5018763493730125750L;

    private String serverId;
    private String serverIp;
    private String serverName;

    private String dbType;
    private String jdbcUrl;
    private String username;
    private String password;


    public String getJdbcType() {
        return StringUtils.defaultString(dbType, "oracle");

    }
}
