package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by juneyoungoh on 2017. 9. 28..
 * 데이터베이스와 노드 정보를 맵핑한다
 * 불편함을 해소하기 위한 유틸리티 컨트롤러
 * 가령, A 서버에서 캐시를 올려서 디비에 쌓았는데, 망이 막혀있어 캐시파일을 공유하지 못하는 경우
 * 테이블명/노드명/dsId 를 파라미터로 디비 데이터를 읽어서 노드에 넣는다.
 */
@Controller
@RequestMapping(value = "dbSync")
public class DBSyncController {

    Logger logger = Logger.getLogger(DBSyncController.class);

    @Autowired
    DBSyncService dbSyncService;

    @RequestMapping(value = {"loadTable/{tid}"}, produces = { "application/json" })
    public @ResponseBody String loadBurden(@PathVariable String tid, HttpServletRequest request) throws Exception {
        JSONObject jobj = new JSONObject();
        String result = "500", result_msg = "", cause = "";
        try {
            String skip = request.getParameter("skip");
            dbSyncService.loadTable2Node(tid, (skip != null && "Y".equals(skip.toUpperCase())));
        } catch (Exception e) {
            logger.error("Failed load table to Node", e);
            cause = e.getMessage();
        }
        jobj.put("result", result);
        jobj.put("result_msg", result_msg);
        jobj.put("cause", cause);
        return String.valueOf(jobj);
    }
}
