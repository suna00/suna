package net.ion.ice.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by seonwoong on 2017. 6. 7..
 */

@Configuration
@ConfigurationProperties(prefix = "jwtConfig")
public class JwtConfig {

    private String issuer;
    private String secretKey;
    private String headerPrefix;
    private String headString;
    private Integer tokenExpirationTime;
    private Integer refreshTokenExpTime;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getHeaderPrefix() {
        return headerPrefix;
    }

    public void setHeaderPrefix(String headerPrefix) {
        this.headerPrefix = headerPrefix;
    }

    public String getHeadString() {
        return headString;
    }

    public void setHeadString(String headString) {
        this.headString = headString;
    }

    public Integer getTokenExpirationTime() {
        return tokenExpirationTime;
    }

    public void setTokenExpirationTime(Integer tokenExpirationTime) {
        this.tokenExpirationTime = tokenExpirationTime;
    }

    public Integer getRefreshTokenExpTime() {
        return refreshTokenExpTime;
    }

    public void setRefreshTokenExpTime(Integer refreshTokenExpTime) {
        this.refreshTokenExpTime = refreshTokenExpTime;
    }
}


