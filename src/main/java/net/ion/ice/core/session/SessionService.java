package net.ion.ice.core.session;

import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.cluster.ClusterConfiguration;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import net.ion.ice.security.auth.jwt.extractor.TokenExtractor;
import net.ion.ice.security.common.CookieUtil;
import net.ion.ice.security.config.JwtConfig;
import net.ion.ice.security.token.JwtTokenFactory;
import net.ion.ice.security.token.RawAccessJwtToken;
import net.ion.ice.security.token.RefreshToken;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SessionService {
    private static Logger logger = LoggerFactory.getLogger(SessionService.class);

    @Autowired
    private ClusterConfiguration clusterConfiguration;
    @Autowired
    private TokenExtractor tokenExtractor;
    @Autowired
    private JwtTokenFactory tokenFactory;
    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private NodeService nodeService ;

    public Map<String, Object> getSession(HttpServletRequest request) throws UnsupportedEncodingException {
        String sessionKey = getSessionKey(request) ;
        if(StringUtils.isEmpty(sessionKey)){
            return null;
        }
        Map<String, Object> sessionMap = clusterConfiguration.getSesssionMap().get(sessionKey);

        return sessionMap;
    }

    public void putSession(HttpServletRequest request, HttpServletResponse response, Map<String, Object> sessionData) throws UnsupportedEncodingException {
        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();

        if (getSession(request) == null) {
            String sessionKey = tokenFactory.createInitJwtToken().getToken();
            String refreshSessionKey = null ;

            if (sessionData == null) {
                refreshSessionKey = tokenFactory.createRefreshToken(false).getToken();
                sessionData = new HashMap<>();
            }else{
                refreshSessionKey = tokenFactory.createRefreshToken(true).getToken();
            }
            sessionMap.put(sessionKey, sessionData);
            CookieUtil.create(response, "iceJWT", jwtConfig.getTokenPrefix().concat(" ").concat(sessionKey), false, false, -1, request.getServerName());
            CookieUtil.create(response, "iceRefreshJWT", jwtConfig.getTokenPrefix().concat(" ").concat(refreshSessionKey), true, false, -1, request.getServerName());
        }else{
            Map<String, Object> session = getSession(request);
            if(sessionData != null){
                session.putAll(sessionData);
            }
        }
    }

    public String createTempSession(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        String sessionKey = tokenFactory.createInitJwtToken().getToken();
        String refreshSessionKey = null ;

        refreshSessionKey = tokenFactory.createRefreshToken(false).getToken();
        String token = jwtConfig.getTokenPrefix().concat(" ").concat(sessionKey) ;
        CookieUtil.create(response, "iceJWT", token, false, false, -1, request.getServerName());
        CookieUtil.create(response, "iceRefreshJWT", jwtConfig.getTokenPrefix().concat(" ").concat(refreshSessionKey), true, false, -1, request.getServerName());
        return token ;
    }

    public void removeSession(HttpServletRequest request) throws UnsupportedEncodingException {
        Map<String, Object> sessionMap = clusterConfiguration.getSesssionMap().get(getSessionKey(request));
        sessionMap.put(getSessionKey(request), null);
    }

    public void refreshSession(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();
        if (getSession(request) != null) {
            String iceRefreshJWTPlayload = CookieUtil.getValue(request, "iceRefreshJWT");
            RawAccessJwtToken rawToken = new RawAccessJwtToken(tokenExtractor.extract(iceRefreshJWTPlayload));
            RefreshToken.create(rawToken, jwtConfig.getSecretKey()).orElseThrow(() -> new RuntimeException());
            String sessionKey = tokenFactory.createInitJwtToken().getToken();
            sessionMap.put(sessionKey, getSession(request));
            removeSession(request);
            CookieUtil.create(response, "iceJWT", jwtConfig.getTokenPrefix().concat(" ").concat(sessionKey), false, false, -1, request.getServerName());
        }else{
            putSession(request, response, null);
        }
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

    public void setSessionValue(HttpServletRequest request, String key, String[] strings) throws UnsupportedEncodingException {
        Map<String, Object> sessionMap = getSession(request);
        sessionMap.put(key, strings[0]);
    }


    public Map<String, Object> userLogin(HttpServletRequest request, HttpServletResponse response, String userId, String password) {
        Node node = nodeService.getNode("user", userId) ;

        if(node == null || !StringUtils.equals(node.getStringValue("password"), password)){
            throw new ApiException("300", "Login Fail") ;
        }

        Map<String, Object> session = new HashMap<>();
        session.put("user", node);
        List<String> roles = new ArrayList<>();

        addRoles(roles, node);

        Node group = node.getReferenceNode("userGroupId");
        if(group != null){
            addRoles(roles, group);
        }

        roles.add("admin") ;
        roles.add("customer") ;
        roles.add("anonymous") ;
        session.put("role", String.join(",", roles));

        logger.info("{} user has roles : {}", userId, session.get("role"));
        try {
            putSession(request, response, session);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return session ;
    }

    private void addRoles(List<String> roles, Node group) {
        for(String role : StringUtils.split(group.getStringValue("role"), ",")){
            if(!roles.contains(role)){
                roles.add(role) ;
            }
        }
    }
}
