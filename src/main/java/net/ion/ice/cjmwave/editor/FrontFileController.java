package net.ion.ice.cjmwave.editor;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 11. 10..
 */
@Controller
@RequestMapping(value = { "frontFile" })
public class FrontFileController {

    @Autowired
    FrontFileService frontFileService;

    private Logger logger = Logger.getLogger(FrontFileController.class);

    @RequestMapping(value = "uploadFile", method = { RequestMethod.POST }, produces = { "application/json" })
    public @ResponseBody String uploadS3EditorFile (MultipartHttpServletRequest request) throws JSONException {
        JSONObject jObj = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";
        List<Map<String, Object>> files = new ArrayList<>();
        try {
            files = frontFileService.uploadFiles(request);

            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error("front file upload failed :: ", e);
            cause = e.getMessage();
        }
        jObj.put("result", result);
        jObj.put("result_msg", result_msg);
        jObj.put("cause", cause);
        jObj.put("files", JSONObject.wrap(files));
        return jObj.toString();
    }
}
