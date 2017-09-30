package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service("pointService")
public class PointService {

    public static final String YPOINT = "YPoint";
    public static final String WELFAREPOINT = "welfarePoint";

    public static final String usedYPointMap = "usedYPointMap";
    public static final String usedWelfarePointMap = "usedWelfarePointMap";

    public static final String MEMBER = "member";
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(CommonService.PATTERN);

    @Autowired
    private NodeService nodeService ;
    @Autowired
    private NodeBindingService nodeBindingService ;
    public NodeBindingInfo nodeBindingInfo;

    public boolean checkUsablePoint(String memberNo, String type, int usePoint){
        Node node = NodeUtils.getNode(MEMBER, memberNo);
        if(Integer.parseInt(node.get(type).toString()) < usePoint) return false;
        return true;
    }

    //type : YPOINT, WDLFAREPOINT
    private Double updateMember(String memberNo, String type, Integer usePoint) {
        Node member = NodeUtils.getNode(MEMBER, memberNo);
        Double point = (member.get(type) == null ? 0 : Double.parseDouble(member.get(type).toString()));
        member.put(type, point + usePoint);
        nodeService.executeNode(member, MEMBER, CommonService.UPDATE);
        return point + usePoint;
    }

    // 유효기간 짧은순으로 순차적 차감
    private void deductInOrder(Integer point, String searchText, String typeTid, String mapTid) {
        Integer temp = point;
        List<Map<String, Object>> useablePointList = nodeBindingService.list(typeTid, searchText);
        for(Map<String, Object> p : useablePointList){
            Integer balance = Integer.parseInt(p.get("balance").toString());
            if(temp >= balance ){
                p.put("balance", 0);
                p.put("usedPrice", balance);
                temp = temp - balance;
            }else{
                p.put("balance", balance - temp);
                p.put("usedPrice", temp);
            }
            nodeService.executeNode(p, typeTid, CommonService.UPDATE);
            nodeService.executeNode(p, mapTid, CommonService.CREATE);

        }
    }

    // [Y포인트 안내]
    // 특정상품 판매 및 특별이벤트 진행할 경우에 부여됩니다.
    public ExecuteContext addYPoint(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        Integer point = Integer.parseInt(data.get(YPOINT).toString());

        Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(data);
        map.put("YPointType", "tobe");  //오늘로부터 7일후 적립 tobe->add
        map.put("description", "상품구매");
        map.put("price", point);
        map.put("balance", point);

        LocalDateTime now = LocalDateTime.now();
        map.put("startDate", now.plusDays(7).format(formatter));
        map.put("endDate", now.plusDays(7).plusYears(1).format(formatter));

        nodeService.executeNode(map, YPOINT, CommonService.CREATE);

        Double havePoint = updateMember(data.get("memberNo").toString(), YPOINT, point);
        Map<String, Object> result = new HashMap<>();
        result.put(YPOINT, havePoint);
        context.setResult(CommonService.getResult("S0002", result));

        return context;
    }

    public ExecuteContext useYPoint(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        Integer point = Integer.parseInt(data.get(YPOINT).toString());

        if(!checkUsablePoint(data.get("memberNo").toString(), YPOINT, point)){
            context.setResult(CommonService.getResult("Y0001"));
            return context;
        }

        deductInOrder(point, "YPointType_equals=add&balance_notEquals=0&sorting=endDate", YPOINT, usedYPointMap);

        // 포인트 사용 이력
        Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(data);
        map.put("YPointType", "use");
        map.put("description", "상품결제");
        map.put("price", -point);
        nodeService.executeNode(map, YPOINT, CommonService.CREATE);

        Double havePoint = updateMember(data.get("memberNo").toString(), YPOINT, -point);
        Map<String, Object> result = new HashMap<>();
        result.put(YPOINT, havePoint);
        context.setResult(CommonService.getResult("S0002", result));

        return context;
    }

    // cancel, return
    public ExecuteContext refundYPoint(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        Integer point = Integer.parseInt(data.get(YPOINT).toString());

        List<Map<String, Object>> usedPointMapList = nodeBindingService.list(usedYPointMap, "orderSheetId_equals="+data.get("orderSheetId"));
        for(Map<String, Object> y : usedPointMapList){
            Node node = NodeUtils.getNode(YPOINT, y.get(YPOINT.concat("Id")).toString());
            node.put("balance", y.get("usedPrice"));
            nodeService.executeNode(node, YPOINT, CommonService.UPDATE);
        }

        String description = "";
        if("cancel".equals(data.get("changeType"))){
            description = "상품취소";
        }else if("exchange".equals(data.get("changeType"))){
            description = "상품반품";
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(data);
        map.put("YPointType", data.get("changeType"));  // orderChange - chageType : cancel, exchange, return
        map.put("description", description);
        map.put("price", point);
        nodeService.executeNode(map, YPOINT, CommonService.CREATE);

        Double havePoint = updateMember(data.get("memberNo").toString(), YPOINT, point);
        Map<String, Object> result = new HashMap<>();
        result.put(YPOINT, havePoint);
        context.setResult(CommonService.getResult("S0002", result));

        return context;
    }

    //소멸(Batch)
    public ExecuteContext removeYPoint(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        Integer point = Integer.parseInt(data.get(YPOINT).toString());

        Integer temp = 0;
        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).concat("000000");
        List<Map<String, Object>> list = nodeBindingService.list(YPOINT, "YPointType_equals=add&balance_notEquals=0&endDate_below=" + now + "&sorting=endDate");
        for(Map<String, Object> y : list){
            Node node = NodeUtils.getNode(YPOINT, y.get("YPointId").toString());
            temp = temp + Integer.parseInt(node.get("balance").toString());
            node.put("balance", 0);
            nodeService.executeNode(node, YPOINT, CommonService.UPDATE);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(data);
        map.put(YPOINT.concat("Type"), "remove");
        map.put("description", "포인트 유효기간 종료");
        map.put("price", temp);
        nodeService.executeNode(map, YPOINT, CommonService.CREATE);

        Double havePoint = updateMember(data.get("memberNo").toString(), YPOINT, -temp);
        Map<String, Object> result = new HashMap<>();
        result.put(YPOINT, havePoint);
        context.setResult(CommonService.getResult("S0002", result));

        return context;
    }

    public ExecuteContext useWelfarePoint(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        Integer point = Integer.parseInt(data.get(WELFAREPOINT).toString());

        if(checkUsablePoint(data.get("memberNo").toString(), WELFAREPOINT, point)){
            context.setResult(CommonService.getResult("W0001"));
            return context;
        }

        deductInOrder(point, "welfarePointType_equals=divide&balance_notEquals=0&sorting=endDate", WELFAREPOINT, usedWelfarePointMap);

        // 포인트 사용 이력
        Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(data);
        map.put("welfarePointType", "use");
        map.put("price", -point);
        nodeService.executeNode(map, WELFAREPOINT, CommonService.CREATE);

        Double havePoint = updateMember(data.get("memberNo").toString(), WELFAREPOINT, -point);
        Map<String, Object> result = new HashMap<>();
        result.put(WELFAREPOINT, havePoint);
        context.setResult(CommonService.getResult("S0002", result));


        return context;
    }

    // cancel, return
    public ExecuteContext refundWelfarePoint(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        Integer point = Integer.parseInt(data.get(WELFAREPOINT).toString());

        List<Map<String, Object>> usedPointMapList = nodeBindingService.list(usedWelfarePointMap, "orderSheetId_equals="+data.get("orderSheetId"));
        for(Map<String, Object> y : usedPointMapList){
            Node node = NodeUtils.getNode(WELFAREPOINT, y.get(WELFAREPOINT.concat("Id")).toString());
            node.put("balance", y.get("usedPrice"));
            nodeService.executeNode(node, WELFAREPOINT, CommonService.UPDATE);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(data);
        map.put(WELFAREPOINT.concat("Type"), data.get("changeType"));  // orderChange - chageType : cancel, exchange, return
        map.put("price", point);
        nodeService.executeNode(map, WELFAREPOINT, CommonService.CREATE);

        Double havePoint = updateMember(data.get("memberNo").toString(), WELFAREPOINT, point);
        Map<String, Object> result = new HashMap<>();
        result.put(WELFAREPOINT, havePoint);
        context.setResult(CommonService.getResult("S0002", result));

        return context;
    }

    //소멸(Batch)
    public ExecuteContext removeWelfarePoint(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        return context;
    }


    public ExecuteContext increaseWelfarePoint(ExecuteContext context) {

        return context;
    }


    public ExecuteContext decreaseWelfarePoint(ExecuteContext context) {

        return context;
    }


    public ExecuteContext divideWelfarePoint(ExecuteContext context) {

        return context;
    }


    public ExecuteContext getbackWelfarePoint(ExecuteContext context) {

        return context;
    }

}
