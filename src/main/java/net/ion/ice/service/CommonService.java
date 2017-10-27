package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommonService {

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String SAVE = "save";
    public static final String PATTERN = "yyyyMMddHHmmss";
    public static final String unlimitedDate = "99991231235959";

    public static final Map<String, Object> resultCodeMap;
    static
    {
        resultCodeMap = new HashMap();
        resultCodeMap.put("S0001", "required param. ");
        resultCodeMap.put("S0002", "저장 성공");
        resultCodeMap.put("S0003", "저장 실패");

        resultCodeMap.put("S0004", "스윗트래커 실패");

        /*product*/
        resultCodeMap.put("P0001", "미승인 상품입니다.");
        resultCodeMap.put("P0002", "전시중인 상품이 아닙니다.");
        resultCodeMap.put("P0003", "삭제된 상품입니다.");
        resultCodeMap.put("P0004", "판매중인 상품이 아닙니다.");
        resultCodeMap.put("P0005", "품절된 상품입니다.");

        /*leave Member*/
        resultCodeMap.put("L0001", "진행중인 거래내역이 있습니다.");
        resultCodeMap.put("L0002", "회원정보가 존재하지 않습니다.");
        resultCodeMap.put("L0003", "이미 탈퇴한 회원입니다.");

        /*Member*/
        resultCodeMap.put("U0001", "중복된 인증 메일입니다.");
        resultCodeMap.put("U0002", "존재하지 않는 인증코드입니다.");
        resultCodeMap.put("U0003", "만료된 인증코드입니다.");
        resultCodeMap.put("U0004", "회원정보가 일치하지 않습니다. 다시 입력해주세요.");
        resultCodeMap.put("U0005", "입력하신 정보와 일치하는 아이디가 없습니다. 다시 입력해주세요.");
        resultCodeMap.put("U0006", "인증번호를 잘못 입력하셨습니다.");
        resultCodeMap.put("U0007", "로그인 성공");
        resultCodeMap.put("U0008", "로그인 실패");
        resultCodeMap.put("U0009", "로그인 사용자");
        resultCodeMap.put("U0010", "비로그인 사용자");
        resultCodeMap.put("U0011", "휴면계정 사용자");
        resultCodeMap.put("U0012", "비밀번호 변경 안내.회원님의 비밀번호 변경이 필요한 시기입니다.");

        /*Coupon*/
        resultCodeMap.put("V0001", "존재하지 않는 쿠폰유형입니다.");
        resultCodeMap.put("V0002", "[발급수제한] 쿠폰 수량이 모두 소진되었습니다.");
        resultCodeMap.put("V0003", "[동일인재발급제한] 받을 수 있는 쿠폰이 없습니다.");
        resultCodeMap.put("V0004", "발급가능한 기간이 만료되었습니다.");
        resultCodeMap.put("V0005", "이미 발급된 쿠폰입니다.");

        /*MyPage*/
        resultCodeMap.put("M0001", "주소록 삭제 성공");
        resultCodeMap.put("M0002", "주문접수, 결제완료인 경우만 배송지 변경이 가능합니다.");
        resultCodeMap.put("M0003", "부분취소를 할 수 없습니다.");
        resultCodeMap.put("M0004", "취소신청을 할 수 없습니다. 주문상태를 확인해 주세요.");
        resultCodeMap.put("M0005", "교환신청을 할 수 없습니다. 주문상태를 확인해 주세요.(SMS 또는 LG/삼성 직배송의 경우 교환불가)");
        resultCodeMap.put("M0006", "반품신청을 할 수 없습니다. 주문상태를 확인해 주세요.(SMS 또는 LG/삼성 직배송의 경우 반품불가)");
        resultCodeMap.put("M0007", "비밀번호가 맞지 않습니다.");
        resultCodeMap.put("M0008", "비밀번호가 5회 잘못 입력하셨습니다.\n개인 정보보호를 위하여 비밀번호를 재설정 해주시기 바랍니다.");
        resultCodeMap.put("M0009", "환불계좌 정보를 입력해주세요.");
        resultCodeMap.put("M0010", "주문변경 신청 성공하였습니다.");


        /*Order*/
        resultCodeMap.put("O0001", "임시 주문서 저장 성공");
        resultCodeMap.put("O0002", "포인트 검증 실패");
        resultCodeMap.put("O0003", "가격 검증 실패");
        resultCodeMap.put("O0004", "가격 검증 성공");
        resultCodeMap.put("O0005", "포인트구매 성공");
        resultCodeMap.put("O0006", "포인트구매 실패");

        /*Cart*/
        resultCodeMap.put("C0001", "상품 삭제 성공");
        resultCodeMap.put("C0002", "수량을 1개 이하로 선택할 수 없습니다.");
        resultCodeMap.put("C0003", "최소 주문가능 수량은 N개 입니다.");
        resultCodeMap.put("C0004", "선택하신 옵션의 재고가 부족합니다.");

        /*YPoint*/
        resultCodeMap.put("Y0001", "Y포인트 잔액이 부족합니다.");

        /*WelfarePoint*/
        resultCodeMap.put("W0001", "복지포인트 잔액이 부족합니다.");

        /*InterestList*/
        resultCodeMap.put("I0001", "위시리스트에 등록되었습니다.");
        resultCodeMap.put("I0002", "이미 등록된 상품 입니다.");

        resultCodeMap.put("CORE#JWT01", "Get JWT Success");
        resultCodeMap.put("CORE#JWT02", "JWT Expired");

    }

    public static boolean requiredParams(ExecuteContext context, Map<String, Object> data, String[] params) {
        for(String str : params){
            if(data.get(str) == null){
                setErrorMessage(context, "S0001", str);
                return true;
            }
        }
        return false;
    }

    public static Object getResult(String code){
        Map<String, Object> object = new HashMap<>();
        Map<String, Object> object2 = new HashMap<>();
        object2.put("code", code);
        object2.put("message", resultCodeMap.get(code));
        object.put("validate", object2);

        return object;
    }

    public static Object getResult(String code, Map<String, Object> extraData){
        Map<String, Object> object = new LinkedMap<>();
        Map<String, Object> object2 = new LinkedMap<>();
        object2.put("code", code);
        object2.putAll(extraData);
        object2.put("message", resultCodeMap.get(code));
        object.put("validate", object2);

        return object;
    }

    public static void setErrorMessage(ExecuteContext context,  String code) {
        Map<String, Object> object = new HashMap<>();
        Map<String, Object> object2 = new HashMap<>();
        object2.put("code", code);
        object2.put("message", resultCodeMap.get(code));
        object.put("validate", object2);
        context.setResult(object);
    }

    public static void setErrorMessage(ExecuteContext context, String code, String message) {
        Map<String, Object> object = new HashMap<>();
        Map<String, Object> object2 = new HashMap<>();
        object2.put("code", code);
        object2.put("message", resultCodeMap.get(code) + " " + message);
        object.put("validate", object2);
        context.setResult(object);
    }

    public static String replaceUrl(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String[] splitRequestUrl = StringUtils.split(request.getRequestURL().toString(), "/");
        return String.format("%s//%s", splitRequestUrl[0], splitRequestUrl[1]); // "http://localhost:8080", "http://125.131.88.206:8080"
    }

    public static Map<String, Object> resetMap(Map<String, Object> map) {
        map.remove("owner");
        map.remove("created");
        map.remove("modifier");
        map.remove("changed");
        return map;
    }


    public static Map<String, Object> putReferenceValue(String nodeTypeId, ExecuteContext context, Map<String, Object> op) {
        NodeType nodeType = NodeUtils.getNodeType(nodeTypeId);
        for (PropertyType pt : nodeType.getPropertyTypes()) {
            if ("REFERENCE".equals(pt.getValueType().toString()) && op.get(pt.getPid()) != null) {
                op.put(pt.getPid(), NodeUtils.getReferenceValue(context, op.get(pt.getPid()), pt));
            }
        }
        return op;
    }
}
