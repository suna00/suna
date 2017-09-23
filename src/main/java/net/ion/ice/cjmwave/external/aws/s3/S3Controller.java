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
        } catch (Exception e) {
            cause = e.getMessage();
        }
        resultMap.put("result", result);
        resultMap.put("result_msg", result_msg);
        resultMap.put("items", s3Files);
        resultMap.put("size", s3Files.size());
        resultMap.put("cause", cause);
        return mapper.writeValueAsString(resultMap);
    }

    /*
    * 디렉토리 지정하면 하위 디렉토리 제외하고 파일을 모두 지움
    * */
    @RequestMapping(value = {"remove"}, produces = {"application/json"})
    public @ResponseBody String removeFiles(HttpServletRequest request) throws Exception{
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> resultMap = new HashMap<>();
        String result = "500", result_msg = "ERROR", cause = "";
        List<Map<String, Object>> removedFiles = new ArrayList<>();

        try {
            String subPath = request.getParameter("path");
            result = "200";
            result_msg = "SUCCESS";
            removedFiles = s3Service.removeFiles(subPath);
        } catch (Exception e) {
            logger.error("Failed to remove S3 files");
        }
        resultMap.put("result", result);
        resultMap.put("result_msg", result_msg);
        resultMap.put("items", removedFiles);
        resultMap.put("size", removedFiles.size());
        resultMap.put("cause", cause);
        return mapper.writeValueAsString(resultMap);
    }


    @RequestMapping("download")
    public void download(){

    }
}
