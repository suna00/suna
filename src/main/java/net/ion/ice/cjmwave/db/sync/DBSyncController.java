package net.ion.ice.cjmwave.db.sync;

import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 1..
 * 0905
 * 디비 접속해서 노드 생성하는 부분은 서비스로 추출되어야 한다.
 * 그래야 스케쥴러에서 서비스를 Autowired 해서 사용가능함
 */
@Deprecated
@Controller
@RequestMapping(value = { "dbSync" })
public class DBSyncController {

    Logger logger = Logger.getLogger(DBSyncController.class);

    private final String PROCESS_TID = "dbSyncProcess"
            , MAPPER_TID = "dbSyncMapper"
            , HISTORY_TID = "dbSyncHistory";


    @Autowired
    DBSyncService dbSyncService;

    @RequestMapping(value = { "/execute/{executeId}" }, produces = {"application/json"})
    public @ResponseBody String execute (@PathVariable String executeId, HttpServletRequest request) throws Exception {

        logger.info("Start executing database Sync process with Id [ " + executeId + " ]");
        JSONObject rtn = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";
        List<Map> altered = new ArrayList<>();
        try{
            altered = dbSyncService.executeJob(executeId, request);
            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error("Database synchronize execution Error :: ", e);
            cause = e.getMessage();
        }
        rtn.put("result", result);
        rtn.put("items", altered);
        rtn.put("result_msg", result_msg);
        rtn.put("cause", cause);
        return rtn.toString();
    }


    // type 은 pip 혹은 mnet 임
    @RequestMapping(value = "/init/{initType}", produces = {"application/json"})
    public @ResponseBody String initialize(@PathVariable String initType, HttpServletRequest request)
            throws Exception {

        logger.info("Initialize type [ " + initType + " ]");
        JSONObject rtn = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";

        String pipExecuteIds[] = {};
        String mnetExecuteIds[] = {
                //"migAlbum",
                "migAlbumMulti", "migArtist", "migArtistMulti"
                , "migSong", "migSongMulti"
                //, "migMusicVideo", "migMusicVideoMulti"
                //, "migGroupPhoto", "migSinglePhoto"
        };

        try{
            String task = request.getParameter("task");
            if(task == null){
                // 해당 타입에 대한 전체 초기화
                switch (initType) {
                    case "pip" :
                        logger.info("impletmet pip migration");
                        break;
                    case "mnet" :
                        logger.info("MNET migration for all data");
                        for(String executeId : mnetExecuteIds) {
                            dbSyncService.executeWithIteration(executeId);
                        }

                        break;
                    default:
                        logger.info("Not identified initType");
                        break;
                }


            } else {
                // 지정 태스크 초기화
            }
            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error("Database synchronize execution Error :: ", e);
            cause = e.getMessage();
        }
        rtn.put("result", result);
        rtn.put("result_msg", result_msg);
        rtn.put("cause", cause);
        return rtn.toString();
    }



    /*
    * 실패 로그를 테이블로 쌓았다고 가정한다
    * 사용자가 실패로그 목록을 Excel 로 받았다고 가정
    * 해당 실패 목록을 재시도 요청할 수 있는 수동 인터페이스
    * */
    @RequestMapping(value = {"/execute/File"}, produces = { "application/json" })
    public @ResponseBody String executeFile(HttpServletRequest request) {
        return null;
    }
}
