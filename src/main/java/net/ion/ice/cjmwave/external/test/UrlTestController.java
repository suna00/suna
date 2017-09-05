package net.ion.ice.cjmwave.external.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.cjmwave.external.UrlCallService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 4..
 */
@Controller
@RequestMapping("pip")
public class UrlTestController {

    private Logger logger = Logger.getLogger(UrlTestController.class);

    @Autowired
    UrlCallService urlCallService;

    @Value("${cjapi.pip.programurl}")
    String programApiUrl;

    @Value("${cjapi.pip.clipmediaurl}")
    String clipMediaApiUrl;


    /*
    * program / clipmedia
    * */
    @RequestMapping(value = "call/{apiType}", produces = {"application/json"})
    public @ResponseBody String call (@PathVariable String apiType, HttpServletRequest request) throws Exception {

        String result = "500", result_msg = "ERROR", cause = "";
        Map<String, Object> rtn = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        List rsList = null;

        logger.info("======================" + apiType);

        try{
            switch (apiType) {
                case "program" :
                    rsList = urlCallService.fetchJSON(programApiUrl, "programid=AA_B120173546");
                    break;
                case "clipmedia" :
                    //AA_B120173546
                    String media_url = clipMediaApiUrl + "?platform=mnetjapan&type=recent";
                    rsList = urlCallService.fetchJSON(media_url, "programid=AA_B120173546&contentid=AA_25532");
                    break;
                default:
                    rsList = new ArrayList();
                    throw new Exception("Unidentified type request");
            }
            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            cause = e.getMessage();
            logger.error("ERROR", e);
        }
        rtn.put("result", result);
        rtn.put("result_msg", result_msg);
        rtn.put("cause", cause);
        rtn.put("api_result", rsList);
        return mapper.writeValueAsString(rtn);
    }
};
