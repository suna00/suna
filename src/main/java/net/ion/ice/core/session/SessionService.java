package net.ion.ice.core.session;

import net.ion.ice.core.cluster.ClusterConfiguration;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import net.ion.ice.security.auth.jwt.extractor.TokenExtractor;
import net.ion.ice.security.common.CookieUtil;
import net.ion.ice.security.config.JwtConfig;
import net.ion.ice.security.token.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.stagemonitor.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private NodeService nodeService ;

    public Map<String, Object> getSession(HttpServletRequest request) throws UnsupportedEncodingException {

        Map<String, Object> sessionMap = clusterConfiguration.getSesssionMap().get(getSessionKey(request));

        return sessionMap;
    }

    public void putSession(HttpServletRequest request, HttpServletResponse response, Map<String, Object> sessionData) throws UnsupportedEncodingException {
        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();

        if (getSession(request) == null) {
            String sessionKey = tokenFactory.createInitJwtToken().getToken();
            String refreshSessionKey = tokenFactory.createRefreshToken().getToken();

            if(sessionData == null) {
                sessionData = new HashMap<>();
            }
            sessionMap.put(sessionKey, sessionData);
            CookieUtil.create(response, "iceJWT", jwtConfig.getTokenPrefix().concat(" ").concat(sessionKey), false, false, -1, request.getServerName());
            CookieUtil.create(response, "iceRefreshJWT", jwtConfig.getTokenPrefix().concat(" ").concat(refreshSessionKey), true, false, -1, request.getServerName());
        }
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


    public void memberLogin(HttpServletRequest request, HttpServletResponse response, String userId, String password, String siteId) {
        if(StringUtils.isEmpty(siteId)){
            siteId = "default" ;
        }
        NodeType memberType = nodeService.getNodeType("member") ;
        List<QueryTerm> queryTerms = new ArrayList<>() ;
        queryTerms.add(QueryUtils.makePropertyQueryTerm(memberType, "siteId", null, siteId)) ;
        queryTerms.add(QueryUtils.makePropertyQueryTerm(memberType, "userId", null, userId)) ;

        List<Node> nodes = nodeService.getNodeList(memberType, queryTerms) ;

        if(nodes == null || nodes.size() ==0){

        }

        Node member = nodes.get(0) ;

        if(!member.getStringValue("password").equals(password)){

        }


        try {
            putSession(request, response, member);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
}
