package net.ion.ice.cjmwave.external.mnet;

import net.ion.ice.cjmwave.db.sync.DBProcessStorage;
import net.ion.ice.cjmwave.db.sync.DBSyncService;
import net.ion.ice.cjmwave.db.sync.ParallelDBSyncExecutor;
import net.ion.ice.cjmwave.external.mnet.data.MnetNodeRecoveryService;
import net.ion.ice.cjmwave.external.mnet.schedule.ScheduledMnetService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;


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

    @Autowired
    ScheduledMnetService scheduledMnetService;

    @Autowired
    MnetNodeRecoveryService mnetNodeRecoveryService;

    @Autowired
    DBProcessStorage storage;


    /*
    * csv 가 MYSQL 로 부어졌다는 전제가 있음
    * mysql 테이블 기준으로 쿼리해서 노드로 밀어넣는다
    * 리스폰스 결과는 API 호출에 대한 응답이며 응답이 200 이더라도 데이터를 보장하지 않는다
    * */
    @RequestMapping(value = {"initialData/{type}"}, produces = { "application/json" })
    public @ResponseBody String retrieveAll(@PathVariable String type, HttpServletRequest request) throws JSONException {
        logger.info("Enter MnetController.retrieveAll type :: " + type);

        JSONObject response = new JSONObject();
        String result="500", result_msg = "ERROR", cause = "";

        try{
            /*
            * 17.10.07
            * startPage 는 배치를 시작할 페이지
            * totalPage 는 총 사용될 배치
            * 타입이 all 일 경우에는 적용되지 않음
            *
            * */

            int start = 0;
            int total = 0;

            String startPage = request.getParameter("startPage");
            String totalPage = request.getParameter("totalPage");
            if(startPage != null) {
                start = Integer.parseInt(startPage.trim());
            }

            if(totalPage != null) {
                total = Integer.parseInt(totalPage.trim());
            }

            dbSyncService.executeForInitData(type, start, total);
            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error("ERROR", e);
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
    @RequestMapping(value = { "renew/{type}" }, produces = {"application/json"})
    public @ResponseBody String retrievePartial (@PathVariable String type, HttpServletRequest request) throws JSONException {
        JSONObject response = new JSONObject();
        String result="500", result_msg = "ERROR", cause = "";
        logger.info("Enter MnetController.retrievePartial type :: " + type);

        try{
            String requestedDate = request.getParameter("date");
            Date provided = null;
            if(requestedDate != null) {
                SimpleDateFormat sdf = null;
                if(requestedDate.length() == 14) {
                    sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                } else if (requestedDate.length() == 8) {
                    sdf = new SimpleDateFormat("yyyyMMdd");
                }
                provided = sdf.parse(requestedDate);
            }

            scheduledMnetService.execute(type, provided);
            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error("Error detected ::", e);
            cause = e.getMessage();
        }

        response.put("result", result);
        response.put("result_msg", result_msg);
        response.put("cause", cause);
        response.put("warning", "This response for API Call. Does not guarantee Data");
        return String.valueOf(response);
    }

    /*
    * NODE_CREATION_FAIL 테이블의 isFixed 가 0 인 정보를 수정하고
    * 성공시 플래그를 1로 처리한다
    *
    * type 이 all 이라면 모든 대상을 처리
    * type 이 file 이라면 FileNotFoundException 에 대한 대상을 처리함
    * seq 를 알고 있다면 해당 seq 의 데이터만 실행할 수 있음
    *
    * file 의 경우 재시도 시에 파일이 없더라도 성공으로 간주하고 노드를 생성한다.
    * */
    @RequestMapping(value = {"recovery/group"}, produces = {"application/json"})
    public @ResponseBody String recoverFailNodes(HttpServletRequest request) throws Exception {
        JSONObject response = new JSONObject();
        String result="500", result_msg = "ERROR", cause = "", caution = "";
        String type = request.getParameter("type");
        logger.info("Enter MnetController.recoverFailNodes type :: " + type);
        try{

            switch (type.toUpperCase()) {
                case "ALL" :
                    mnetNodeRecoveryService.recoverNodeAll();
                    break;
                case "FILE" :
                    caution = "Consider as success if image is missing";
                    mnetNodeRecoveryService.recoverNodeByException("FileNotFoundException");
                    break;
                default:
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
        response.put("caution", caution);
        response.put("warning", "This response for API Call. Does not guarantee Data");
        return String.valueOf(response);
    }

    @RequestMapping(value = {"recovery/single"}, produces = {"application/json"})
    public @ResponseBody String recoverSingleNodes(HttpServletRequest request) throws Exception {
        JSONObject response = new JSONObject();
        String result="500", result_msg = "ERROR", cause = "", caution = "";
        try{
            mnetNodeRecoveryService.recoverNodeBySeq(request.getParameter("seq"));
            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error("Error detected ::", e);
            cause = e.getMessage();
        }
        response.put("result", result);
        response.put("result_msg", result_msg);
        response.put("cause", cause);
        response.put("caution", caution);
        response.put("warning", "This response for API Call. Does not guarantee Data");
        return String.valueOf(response);
    }
}
