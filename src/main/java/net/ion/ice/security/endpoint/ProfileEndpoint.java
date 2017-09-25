package net.ion.ice.security.endpoint;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.security.auth.JwtAuthenticationToken;
import net.ion.ice.security.User.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
public class ProfileEndpoint {
    @Autowired
    private NodeService nodeService;

    @RequestMapping(value="/api/me", method= RequestMethod.GET)
    public @ResponseBody
    Node get(JwtAuthenticationToken token, HttpSession httpSession, HttpServletRequest request) {
        Node node = nodeService.getNode("user", ((UserContext) token.getPrincipal()).getUserId());
        System.out.println("sessionID::::\t" +  httpSession.getId());
        return node;
    }

}
