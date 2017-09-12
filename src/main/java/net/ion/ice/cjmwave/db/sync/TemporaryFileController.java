package net.ion.ice.cjmwave.db.sync;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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

    @Value("${temp-file.dir}")
    String baseDirectory;

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
}
