package net.ion.ice.security.token;

import io.jsonwebtoken.*;
import net.ion.ice.security.common.ErrorCode;
import net.ion.ice.security.common.JwtExpiredTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;

public class RawAccessJwtToken implements JwtToken {
    private static Logger logger = LoggerFactory.getLogger(RawAccessJwtToken.class);
            
    private String token;
    
    public RawAccessJwtToken(String token) {
        this.token = token;
    }

    public Jws<Claims> tokenParseClaims(String signingKey) {
        try {
            return Jwts.parser().setSigningKey(signingKey).parseClaimsJws(this.token);
        } catch (NullPointerException ex) {
            logger.error("Session JWT Token not Exist", ex);
            throw new NullPointerException("Session JWT Token not Exist");
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException | SignatureException ex) {
            logger.error("Invalid JWT Token", ex);
            throw new BadCredentialsException("Invalid JWT token: ", ex);
        } catch (ExpiredJwtException expiredEx) {
            logger.info("JWT Token is expired", expiredEx);
            throw new JwtExpiredTokenException(this, "JWT Token expired", ErrorCode.JWT_TOKEN_EXPIRED, expiredEx);
        }
    }

    @Override
    public String getToken() {
        return token;
    }
}
