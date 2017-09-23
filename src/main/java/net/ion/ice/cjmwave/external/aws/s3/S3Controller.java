package net.ion.ice.cjmwave.external.aws.s3;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by juneyoungoh on 2017. 9. 23..
 */
//@Controller
//@RequestMapping(value = {"test/s3"})
public class S3Controller {

    private Logger logger = Logger.getLogger(S3Controller.class);

//    @RequestMapping("list")
    public void printS3Entries(HttpServletRequest request){

    }

//    @RequestMapping("remove")
    public void removeFiles(HttpServletRequest request){
        try {

        } catch (Exception e) {
            logger.error("Failed to remove S3 files");
        }
    }


//    @RequestMapping("download")
    public void download(){

    }
}
