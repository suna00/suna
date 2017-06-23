package net.ion.ice.core.data;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by seonwoong on 2017. 6. 22..
 */

@Data
public class DataConfiguration implements Serializable {
    private static final long serialVersionUID = 5018763493730125750L;

    private String jdbcType;
    private String url;
    private String user;
    private String password;
}
