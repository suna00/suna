package net.ion.ice.security.common;

import net.ion.ice.security.token.JwtToken;
import org.springframework.security.core.AuthenticationException;

/**
 * Created by seonwoong on 2017. 6. 15..
 */
public class JwtExpiredTokenException extends AuthenticationException {
    private static final long serialVersionUID = -5959543783324224864L;

    private JwtToken token;
    private ErrorCode code;

    public JwtExpiredTokenException(String msg, ErrorCode code) {
        super(msg);
        this.code = code;
    }

    public JwtExpiredTokenException(JwtToken token, String msg, ErrorCode code, Throwable t) {
        super(msg, t);
        this.token = token;
        this.code = code;
    }

    public String token() {
        return this.token.getToken();
    }
}
