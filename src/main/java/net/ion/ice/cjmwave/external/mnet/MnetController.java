package net.ion.ice.cjmwave.external.mnet;

import net.ion.ice.cjmwave.db.sync.DBSyncService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * Created by juneyoungoh on 2017. 9. 14..
 * 전반적으로 다국어 데이터에 대한 정의가 미흡함
 */
@Controller
@RequestMapping(value = {"migration/mnet"})
public class MnetController {

    private Logger logger = Logger.getLogger(MnetController.class);

    @Autowired
    DBSyncService dbSyncService;

    /*
    * csv 가 MYSQL 로 부어졌다는 전제가 있음
    * mysql 테이블 기준으로 쿼리해서 노드로 밀어넣는다
    * 리스폰스 결과는 API 호출에 대한 응답이며 응답이 200 이더라도 데이터를 보장하지 않는다
    * */
    @RequestMapping(value = {"initialData/{type}"}, produces = { "application/json" })
    public @ResponseBody String retrieveAll(@PathVariable String type) throws JSONException {
        String mnetExecuteIds[] = {
                "migAlbum", "migAlbumMulti"
                , "migArtist", "migArtistMulti"
                , "migMusicVideo", "migMusicVideoMulti"
                , "migSong", "migSongMulti"
        };

        JSONObject response = new JSONObject();
        String result="500", result_msg = "ERROR", cause = "";

        try{
            switch (type) {
                case "all" :
                    for(String executeId : mnetExecuteIds) {
                        dbSyncService.executeWithIteration(executeId);
                    }
                    break;
                case "album" :
                    dbSyncService.executeWithIteration("migAlbum");
                    dbSyncService.executeWithIteration("migAlbumMulti");
                    break;
                case "artist" :
                    dbSyncService.executeWithIteration("migArtist");
                    dbSyncService.executeWithIteration("migArtistMulti");
                    break;
                case "song" :
                    dbSyncService.executeWithIteration("migSong");
                    dbSyncService.executeWithIteration("migSongMulti");
                    break;
                case "mv" :
                    dbSyncService.executeWithIteration("migMusicVideo");
                    dbSyncService.executeWithIteration("migMusicVideoMulti");
                    break;
                default:
                    logger.info("Could not find appropriate type for migration");
                    break;
            }

            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            cause = e.getMessage();
        }

        response.put("result", result);
        response.put("result_msg", result_msg);
        response.put("cause", cause);
        response.put("warning", "This response for API Call. Does not guarantee Data.");
        return String.valueOf(response);
    }

    /*
    * 이 부분은 결과 테이블에서 최종 실행일을 찾아 실행일 기준 이후의 data 를 mnet mssql 에서
    * 끌어오는 부분임
    * 0914 여전히 mssql 접속 정보는 없음
    * */
    @RequestMapping(value = { "renew/{type}" })
    public @ResponseBody String retrievePartial (@PathVariable String type) throws JSONException {
        String mnetExecuteIds[] = {
                "migAlbumProcedure", "migAlbumMultiProcedure"
                , "migArtistProcedure", "migArtistMultiProcedure"
                , "migSongProcedure", "migSongMultiProcedure"
                , "migMusicVideoProcedure", "migMusicVideoMultiProcedure"
        };

        JSONObject response = new JSONObject();
        String result="500", result_msg = "ERROR", cause = "";

        try{
            switch (type) {
                case "all" :
                    for(String executeId : mnetExecuteIds) {
                        dbSyncService.executeForNewData("mnet", executeId);
                    }
                    break;
                case "album" :
                    dbSyncService.executeForNewData("mnet", "migAlbum");
                    dbSyncService.executeForNewData("mnet", "migAlbumMulti");
                    break;
                case "artist" :
                    dbSyncService.executeForNewData("mnet", "migArtist");
                    dbSyncService.executeForNewData("mnet", "migArtistMulti");
                    break;
                case "song" :
                    dbSyncService.executeForNewData("mnet", "migSong");
                    dbSyncService.executeForNewData("mnet", "migSongMulti");
                    break;
                case "mv" :
                    dbSyncService.executeForNewData("mnet", "migMusicVideo");
                    dbSyncService.executeForNewData("mnet", "migMusicVideoMulti");
                    break;
                default:
                    logger.info("Could not find appropriate type for migration");
                    break;
            }

            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error("Error detected ::", e);
            cause = e.getMessage();
        }

        response.put("result", result);
        response.put("result_msg", result_msg);
        response.put("cause", cause);
        response.put("warning", "This response for API Call. Does not guarantee Data.");
        return String.valueOf(response);
    }
}
