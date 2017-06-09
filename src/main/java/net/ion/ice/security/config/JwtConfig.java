package net.ion.ice.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by seonwoong on 2017. 6. 7..
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "jwtConfig")
public class JwtConfig {

    private String issuer;
    private String secretKey;
    private String headerPrefix;
    private String headString;
    private Integer tokenExpirationTime;
    private Integer refreshTokenExpTime;

}


