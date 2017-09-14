package net.ion.ice.cjmwave.external.pip;

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
 */

@Controller
@RequestMapping(value = {"migration/pip"})
public class PipController {

    private Logger logger = Logger.getLogger(PipController.class);

    @Autowired
    PipApiService pipService;

    @Autowired
    DBSyncService dbSyncService;

    /*
    * PIP 초기 데이터를 받아와서 MYSQL 로 밀어넣었다는 전제
    * 14 일 기준 program 이랑 pgmVideo 와 맵핑하는 조건으로 들어감
    * 안맞는 데이터 / 다국어 데이터 어떻게 처리할 것인지 논의가 필요함
    *
    * 여튼 로직 상으로 이 부분은 URL 커넥션과 무관하고 DB to Node 임
    * */
    @RequestMapping(value = "initialData/{type}", produces = { "application/json" })
    public @ResponseBody String retrieveAll(@PathVariable String type) throws JSONException {
        JSONObject response = new JSONObject();
        String result="500", result_msg = "ERROR", cause = "";
        try{
            switch (type) {
                case "all" :
                    dbSyncService.executeWithIteration("pipProgram");
                    dbSyncService.executeWithIteration("pipClipMedia");
                    break;
                case "program" :
                    dbSyncService.executeWithIteration("pipProgram");
                    break;
                case "clipMedia" :
                    dbSyncService.executeWithIteration("pipClipMedia");
                    break;
                default:
                    logger.info("No suitable type for migration");
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
    * recent 로 API 를 호출하여 최근 72 시간 내 데이터를 받아와서 node 로 변환함
    * 사실 service 가 List 를 반환하면 안되고, 리스트를 Node 로 만들어야 함
    * */
    @RequestMapping(value = "renew/{type}", produces = { "application/json" })
    public @ResponseBody String retrievePartial(@PathVariable String type) throws JSONException {

        JSONObject response = new JSONObject();
        String result="500", result_msg = "ERROR", cause = "";
        try{
            switch (type) {
                case "all" :
                    pipService.doProgramMigration("type=recent");
                    pipService.doClipMediaMigration("type=recent");
                    break;
                case "program" :
                    pipService.doProgramMigration("type=recent");
                    break;
                case "clipMedia" :
                    pipService.doClipMediaMigration("type=recent");
                    break;
                default:
                    logger.info("No suitable type for migration");
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
}
