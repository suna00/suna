package net.ion.ice.cjmwave.external.aws.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 23..
 */
@Controller
@RequestMapping(value = {"test/s3"})
public class S3Controller {

    private Logger logger = Logger.getLogger(S3Controller.class);

    @Autowired
    S3Service s3Service;


    @RequestMapping(value = {"list"}, produces = {"application/json"})
    public @ResponseBody String printS3Entries(HttpServletRequest request) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> resultMap = new HashMap<>();
        String result = "500", result_msg = "ERROR", cause = "";
        List<Map<String, Object>> s3Files = new ArrayList<>();
        try{
            String subPath = request.getParameter("path");
            s3Files = s3Service.retrieveObjectList(subPath);
            result = "200";
            result_msg = "SUCCESS";
            resultMap.put("items", s3Files);
        } catch (Exception e) {
            cause = e.getMessage();
        }
        resultMap.put("result", result);
        resultMap.put("result_msg", result_msg);
        resultMap.put("size", s3Files.size());
        resultMap.put("cause", cause);
        return mapper.writeValueAsString(resultMap);
    }

    @RequestMapping("remove")
    public void removeFiles(HttpServletRequest request){
        try {

        } catch (Exception e) {
            logger.error("Failed to remove S3 files");
        }
    }


    @RequestMapping("download")
    public void download(){

    }
}
