package net.ion.ice.core.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by jaeho on 2017. 6. 22..
 */
@RestController
public class DatabaseController {

    private static Logger logger = LoggerFactory.getLogger(DatabaseController.class);

    @Autowired
    private DatabaseService databaseService;

    @RequestMapping(value = "/data/query/{dsId}", method = RequestMethod.GET)
    @ResponseBody
    public Object query(WebRequest request, HttpServletResponse response, @PathVariable String dsId, @RequestParam(value="query") String query) throws IOException {
        try{
            databaseService.executeQuery(dsId, query, response);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
