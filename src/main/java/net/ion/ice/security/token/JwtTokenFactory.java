package net.ion.ice.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import net.ion.ice.security.config.JwtConfig;
import net.ion.ice.security.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenFactory {
    private final JwtConfig jwtConfig;
    private Date infinityExpire = DateUtils.parseDate("21000101", "yyyyMMdd") ;

    @Autowired
    public JwtTokenFactory(JwtConfig jwtConfig) throws ParseException {
        this.jwtConfig = jwtConfig;
    }

    public AccessJwtToken createInitJwtToken() {
        Claims claims = Jwts.claims();
        claims.setSubject("Anonymous");

        LocalDateTime currentTime = LocalDateTime.now();

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(Date.from(currentTime.atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(Date.from(currentTime.plusMinutes(jwtConfig.getTokenExpirationTime()).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS512, jwtConfig.getSecretKey())
                .compact();

        return new AccessJwtToken(token, claims);
    }


    public JwtToken createRefreshToken(boolean signed) {
//        if (StringUtils.isBlank(userContext.getUserId())) {
//            throw new IllegalArgumentException("Cannot create JWT Token without userId");
//        }

        LocalDateTime currentTime = LocalDateTime.now();

//        Claims claims = Jwts.claims().setSubject(userContext.getUserId());
        Claims claims = Jwts.claims().setSubject(signed ? "Customer" : "Anonymous");
//        claims.put("scopes", Arrays.asList(Scopes.REFRESH_TOKEN.authority()));
        Date expireDate = signed ? Date.from(currentTime.plusMinutes(jwtConfig.getRefreshTokenExpTime()).atZone(ZoneId.systemDefault()).toInstant()) : infinityExpire ;
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuer(jwtConfig.getIssuer())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(currentTime.atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, jwtConfig.getSecretKey())
                .compact();

        return new AccessJwtToken(token, claims);
    }
}
