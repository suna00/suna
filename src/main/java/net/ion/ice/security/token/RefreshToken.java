package net.ion.ice.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import java.util.Optional;

public class RefreshToken implements JwtToken {
    private Jws<Claims> claims;

    private RefreshToken(Jws<Claims> claims) {
        this.claims = claims;
    }

    public static Optional<RefreshToken> create(RawAccessJwtToken token, String signingKey) {
        Jws<Claims> claims = token.tokenParseClaims(signingKey);

        return Optional.of(new RefreshToken(claims));
    }

    @Override
    public String getToken() {
        return null;
    }

    public Jws<Claims> getClaims() {
        return claims;
    }
    
    public String getJti() {
        return claims.getBody().getId();
    }

    public String getName() {
        return (String) claims.getBody().get("name");
    }

    public String getSubject() {
        return claims.getBody().getSubject();
    }
}
