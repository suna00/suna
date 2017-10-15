package net.ion.ice.security.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by seonwoong on 2017. 6. 14..
 */
public class CookieUtil {
    public static void create(HttpServletResponse response, String name, String value, Boolean httpOnly , Boolean secure, Integer maxAge, String domain) throws UnsupportedEncodingException {
        Cookie cookie = new Cookie(name, URLEncoder.encode(value, "UTF-8"));
        if(domain != null) {
            String[] domains = StringUtils.split(domain, ".");
            if (domains != null && domains.length == 3) {
                domain = domains[1] + "/" + domains[2];
            }
            cookie.setDomain(domain);
        }
        cookie.setPath("/");
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAge);

        response.addCookie(cookie);
    }

    public static void clear(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    public static String getValue(HttpServletRequest httpServletRequest, String name) throws UnsupportedEncodingException {
        Cookie cookie = WebUtils.getCookie(httpServletRequest, name);
        return cookie != null ? URLDecoder.decode(cookie.getValue(), "UTF-8") : null;
    }
}
