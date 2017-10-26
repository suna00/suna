package net.ion.ice.cjmwave.monitor;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by juneyoungoh on 2017. 10. 25..
 */

/*
* 고객 요구사항
* 1. 날짜별 신규 가입자 수
* 2. 큐텐 신규 가입자 수
* 3. 일자별 투표건수 (최근 일주일)
* 4. 국가별 회원 가입자 수
* 5. 큐텐 일자별 투표건수
*
* 개발팀 요구사항 콜 제한
* - 최근 1시간 이내 접근 제한
* - 반드시 레플리카 디비에서 댕겨오기
* */
@Controller(value = "monitorStats")
public class MnetStatsMonitorController {

    Logger logger = Logger.getLogger(MnetStatsMonitorService.class);

    @Autowired
    MnetStatsMonitorService mnetStatsMonitorService;

    @RequestMapping(value = {"voteCnt"}, produces = {"application/json"})
    public @ResponseBody String getVoteCnt (HttpServletRequest request) throws JSONException {

        JSONObject rtn = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";

        try{
            String q10 = request.getParameter("q10");
            boolean isQ10 = q10 != null && q10.equals("true");

            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error(e);
            cause = e.getMessage();
        }
        rtn.put("result", result);
        rtn.put("result_msg", result_msg);
        rtn.put("cause", cause);
        return String.valueOf(rtn);

    }

    @RequestMapping(value = {"newMemberCnt"}, produces = {"application/json"})
    public @ResponseBody String getNewMemberCnt(HttpServletRequest request) throws JSONException {
        JSONObject rtn = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";

        try{
            String q10 = request.getParameter("q10");
            boolean isQ10 = q10 != null && q10.equals("true");

            String country = request.getParameter("useCountry");
            boolean useCountry = country !=  null && "true".equals(country);

            // 로직

            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error(e);
            cause = e.getMessage();
        }
        rtn.put("result", result);
        rtn.put("result_msg", result_msg);
        rtn.put("cause", cause);
        return String.valueOf(rtn);
    }
}
