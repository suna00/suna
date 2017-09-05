package net.ion.ice.cjmwave.external.pip;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Created by juneyoungoh on 2017. 9. 5..
 * 파라미터 어떤식으로 끊어낼건지 고민이 필요함
 */
@Controller
@RequestMapping("pip")
public class PipApiController {

    private Logger logger = Logger.getLogger(PipApiService.class);

    @Autowired
    PipApiService pipApiService;

    @RequestMapping(value = "call/{apiType}", produces = {"application/json"})
    public @ResponseBody String call (@PathVariable String apiType, HttpServletRequest request) throws Exception {

        String result = "500", result_msg = "ERROR", cause = "";
        Map<String, Object> rtn = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        List rsList = null;

        try{
            switch (apiType) {
                case "program" :
                    rsList = pipApiService.fetchProgram ("programid=AA_B120173546");
                    break;
                case "clipmedia" :
                    rsList = pipApiService.fetchClipMedia("platform=mnetjapan&type=recent&programid=AA_B120173546&contentid=AA_25532");
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
}
