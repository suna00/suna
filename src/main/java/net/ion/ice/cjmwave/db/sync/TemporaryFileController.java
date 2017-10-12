package net.ion.ice.cjmwave.db.sync;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by juneyoungoh on 2017. 9. 12..
 */
@Controller
@RequestMapping(value = { "tempFile" })
public class TemporaryFileController {

    private Logger logger = Logger.getLogger(TemporaryFileController.class);
    String baseDirectory;

    @Autowired
    Environment env;

    @Autowired
    TemporaryFileService temporaryFileService;

    @PostConstruct
    public void init(){
        try {
            baseDirectory =  env.getProperty("temp-file.dir");
        } catch (Exception e) {
            logger.error("Error :: " + e.getClass().getName());
        }
    }


    @RequestMapping(value = { "/download" })
    public void download (HttpServletRequest request, HttpServletResponse resp) throws Exception {
        String fName = request.getParameter("fName");
        String fullPath = baseDirectory + fName;
        logger.info("Reading ... " + fullPath);
        File f = new File(baseDirectory + fName);

        resp.setHeader("Content-Transfer-Encoding", "binary");
        resp.setHeader("Content-Disposition","attachment; filename=\"" + fName + "\"");

        InputStream fis = new FileInputStream(f);
        IOUtils.copy(fis, resp.getOutputStream());
        resp.flushBuffer();
    }


    @RequestMapping(value = {"/loadCSV"}, produces = { "application/json" })
    public @ResponseBody String loadCsv(HttpServletRequest request) throws Exception {
        String result = "500", result_msg = "ERROR", cause = "";
        JSONObject jsonObject = new JSONObject();
        try {
            //CSV 읽어서 노드로 만들기
            String targetNodeType = request.getParameter("nodeType");
            String fullPath = request.getParameter("csv");
            temporaryFileService.registerNodeFromCSV(targetNodeType, fullPath);
            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            cause = e.getClass().getName();
            e.printStackTrace();
        }
        logger.info("process done");
        jsonObject.put("result", result);
        jsonObject.put("result_msg", result_msg);
        jsonObject.put("cause", cause);
        return String.valueOf(jsonObject);
    }
}
