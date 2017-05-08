package net.ion.ice.core.cluster;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

/**
 * Created by jaeho on 2017. 3. 29..
 */
@Controller
public class ClusterController {
    @Autowired
    private ClusterConfiguration config ;

    @RequestMapping(value = "/cluster")
    public String index(HttpSession httpSession) {
        config.getMembers() ;

        Integer hits = (Integer) httpSession.getAttribute("hits");

//        LOGGER.info("index() called, hits was '{}', session id '{}'", hits, httpSession.getId());

        if (hits == null) {
            hits = 0;
        }

        httpSession.setAttribute("hits", ++hits);

        return "index";
    }

}
