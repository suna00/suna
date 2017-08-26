package net.ion.ice.core.session;

import net.ion.ice.core.cluster.ClusterConfiguration;
import net.ion.ice.security.auth.jwt.extractor.TokenExtractor;
import net.ion.ice.security.common.CookieUtil;
import net.ion.ice.security.config.JwtConfig;
import net.ion.ice.security.token.JwtTokenFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Service
public class SessionService {
    @Autowired
    private ClusterConfiguration clusterConfiguration;
    @Autowired
    private TokenExtractor tokenExtractor;
    @Autowired
    private JwtTokenFactory tokenFactory;
    @Autowired
    private JwtConfig jwtConfig;

    public Map<String, Object> getSession(HttpServletRequest request) throws UnsupportedEncodingException {

        Map<String, Object> sessionMap = clusterConfiguration.getSesssionMap().get(getSessionKey(request));

        return sessionMap;
    }

    public String putSession(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();
        String sessionKey = "";

        if (getSession(request) == null) {
            sessionKey = tokenFactory.createInitJwtToken().getToken();
            Map<String, Object> data = new HashMap<>();
            sessionMap.put(sessionKey, data);
            CookieUtil.create(response, "iceJWT", jwtConfig.getTokenPrefix().concat(" ").concat(sessionKey), false, false, -1, request.getServerName());
        }
        String jwt = jwtConfig.getTokenPrefix().concat(" ").concat(sessionKey);

        return jwt;
    }

    public String getSessionKey(HttpServletRequest request) throws UnsupportedEncodingException {
        String jwt = "";
        String playLoad = CookieUtil.getValue(request, "iceJWT");

        if (playLoad != null) {
            jwt = tokenExtractor.extract(playLoad);
        }

        return jwt;
    }

    public Object getSessionValue(HttpServletRequest request, String valueKey) throws UnsupportedEncodingException {
        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();
        String jwt = "";
        String playLoad = CookieUtil.getValue(request, "iceJWT");

        if (playLoad != null) {
            jwt = tokenExtractor.extract(playLoad);
        }
        return sessionMap.get(jwt).get(valueKey);
    }

    public Object getSessionValue(String sessionKey, String valueKey) {
        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();
        return sessionMap.get(sessionKey).get(valueKey);
    }
}
