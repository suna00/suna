package net.ion.ice.security.auth.jwt.extractor;

import net.ion.ice.security.config.JwtConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

@Component
public class JwtHeaderTokenExtractor implements TokenExtractor {
    @Autowired
    private JwtConfig jwtConfig;

    @Override
    public String extract(String header) {
        if (StringUtils.isBlank(header)) {
            throw new AuthenticationServiceException("Authorization token cannot be blank!");
        }

        if (!header.substring(0, jwtConfig.getHeaderPrefix().concat(" ").length()).equalsIgnoreCase(jwtConfig.getHeaderPrefix().concat(" "))) {
            throw new AuthenticationServiceException("different prefix header.");
        }

        if (header.length() < jwtConfig.getHeaderPrefix().concat(" ").length()) {
            throw new AuthenticationServiceException("Invalid authorization header size.");
        }

        return header.substring(jwtConfig.getHeaderPrefix().concat(" ").length(), header.length());
    }
}
