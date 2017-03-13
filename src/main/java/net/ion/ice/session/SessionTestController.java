package net.ion.ice.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

/**
 * Created by jaehocho on 2017. 3. 10..
 */
@Controller
public class SessionTestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionTestController.class);

    /**
     * Each time the "{@code /}" URL is called, increment the hit counter
     * and indicate that the "{@code index.html}" page should be returned.
     *
     * @param httpSession The current session
     * @return The view to render, in MVC terms.
     */
    @RequestMapping(value = "/")
    public String index(HttpSession httpSession) {

        Integer hits = (Integer) httpSession.getAttribute("hits");

        LOGGER.info("index() called, hits was '{}', session id '{}'", hits, httpSession.getId());

        if (hits == null) {
            hits = 0;
        }

        httpSession.setAttribute("hits", ++hits);

        return "index";
    }
}
