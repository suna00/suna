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

import javax.servlet.http.HttpServletRequest;

/**
 * Created by juneyoungoh on 2017. 9. 14..
 * 구상할 때 규칙
 * - 파일을 당겨오지 못하더라도 에러는 아니다 (파일 프로퍼티에 null)
 *
 */

@Controller
@RequestMapping(value = {"migration/pip"})
public class PipController {

    private Logger logger = Logger.getLogger(PipController.class);
    private final String PLATFORM = "mnetjapan";

    @Autowired
    PipApiService pipService;

    @Autowired
    DBSyncService dbSyncService;

    /*
    * PIP 초기 데이터를 받아와서 MYSQL 로 밀어넣었다는 전제
    * 17 일 기준 all 로 댕겨야 할 수도 있다는 이야기
    * 19 일 그냥 all 로 호출 처리 가정하고 갱신
    * */
    @RequestMapping(value = "initialData/{type}", produces = { "application/json" })
    public @ResponseBody String retrieveAll(@PathVariable String type, HttpServletRequest request) throws JSONException {
        JSONObject response = new JSONObject();
        String result="500", result_msg = "ERROR", cause = "";
        try{
            String saveParam = request.getParameter("save");
            boolean save = (saveParam != null && "Y".equals(saveParam.toUpperCase()));
            switch (type) {
                case "all" :
                    // 데이터가 너무 크면 어떻게 함 ????
                    pipService.doProgramMigration("type=all", save);
                    pipService.doClipMediaMigration("type=all&platform=" + PLATFORM, save);
                    break;
                case "program" :
                    pipService.doProgramMigration("type=all", save);
                    break;
                case "clipMedia" :
                    pipService.doClipMediaMigration("type=all&platform=" + PLATFORM, save);
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
    public @ResponseBody String retrievePartial(@PathVariable String type, HttpServletRequest request) throws JSONException {

        JSONObject response = new JSONObject();
        String result="500", result_msg = "ERROR", cause = "";
        try{
            String saveParam = request.getParameter("save");
            boolean save = (saveParam != null && "Y".equals(saveParam.toUpperCase()));

            switch (type) {
                case "all" :
                    pipService.doProgramMigration("type=recent", save);
                    pipService.doClipMediaMigration("type=recent&platform=" + PLATFORM, save);
                    break;
                case "program" :
                    pipService.doProgramMigration("type=recent", save);
                    break;
                case "clipMedia" :
                    pipService.doClipMediaMigration("type=recent&platform=" + PLATFORM, save);
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
