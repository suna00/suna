package net.ion.ice.cjmwave.db.sync.utils;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by juneyoungoh on 2017. 9. 1..
 * 파라미터 문자열을 파싱한다
 * 가령 @{DATETIME.created} 와 같이 넘어올 수 있다
 * @{데이터 타입.http 파라미터 명} : 데이터 타입 생략시 디폴트는 문자열이다
 *
 * 데이터 타입처리는 다음 기회에...
 */
public class SyntaxUtils {

    private static final String OPENER = "@{", CLOSER = "}";

    /*
    *   결과
    *   query : String
    *   params : Object[]
    *   이렇게 return 하면 jdbcTemplate 사용시
    *   template.queryForList(String.valueOf(map.get("query")), map.get("params")) 로 사용 가능
    * */
    public static Map<String, Object> parse (String query, HttpServletRequest request) throws Exception {
        Map<String, Object> rtn = new HashMap<>();
        /*
        * pseudo - 절차
        * 1. 쿼리 문자열이 @{} 를 포함한다면 내부 컨텐츠를 문자열 배열로 저장한다
        * 2. 문자열 배열의 맴버 중 . 이 포함한다면 0 번째를 타입, 2번째를 request파라미터명으로 간주한다
        * 3. 1 결과 중 변수처리 된 부분을 ? 로 치환한다.
        * 4. 2 결과에 맞게 순차적으로 오브젝트 배열을 생성한다.
        * 5. 맵 반환
        * */
        String patternedQuery = query;
        List<Object> temp = new ArrayList<>();

        // 하나라도 대상이 있을 때만 파싱하기
        if(query.contains(OPENER)) {
            Map<String, Object> parsed = extractParams(query);
            List<String> parsedKeys = (List<String>) parsed.get("parameterKeys");
            for(String paramKey : parsedKeys) {
                Object value = null;
                if(paramKey.contains(".")) {
                    // 데이터 타입 파싱
                    String [] paramKeyParts = paramKey.split("\\.");
                    String dataType = paramKeyParts[0].toUpperCase().trim();
                    value = request.getParameter(paramKeyParts[1]).trim();
                    switch (dataType) {
                        case "BIGINT":
                        case "INT":
                            value = Integer.parseInt(value.toString());
                            break;
                        case "DATETIME":
                        case "TIMESTAMP":
                            SimpleDateFormat dFormat = null;
                            if(value.toString().length() == 14) {
                                dFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                            } else if (value.toString().length() == 8) {
                                dFormat = new SimpleDateFormat("yyyyMMdd");
                            }
                            value = dFormat.parse(value.toString());
                            break;
                        default:
                            value = value.toString();
                        break;
                    }
                }else {
                    value = request.getParameter(paramKey);
                }
                temp.add(value);
            }
            // @{} 문자열 전부 ? 로 치환
            List<String> replaceToQuestionMark = (List<String>) parsed.get("replaces");
            for(String target : replaceToQuestionMark) {
                patternedQuery = patternedQuery.replace(target, "?");
            }
        }

        rtn.put("query", patternedQuery);
        rtn.put("params", temp.toArray());
        return rtn;
    }

    private static Map<String, Object> extractParams (String originalStr) {

        Map<String, Object> queryAndParams = new HashMap<>();

        List<String> parsed = new ArrayList<>();
        List<String> willReplaced = new ArrayList<>();

        Pattern open = Pattern.compile("@\\{");
        Matcher oMatcher = open.matcher(originalStr);

        Pattern close = Pattern.compile("}");
        Matcher cMatcher = close.matcher(originalStr);

        while(oMatcher.find()) {
            if(cMatcher.find()) {
                parsed.add(originalStr.substring(oMatcher.start() + 2, cMatcher.start()));  //@{BIGINT.start}, @{DATETIME.created}
                String toChange = originalStr.substring(oMatcher.start(), cMatcher.start() + 1);
                willReplaced.add(toChange);
            }
        }
        queryAndParams.put("parameterKeys", parsed);
        queryAndParams.put("replaces", willReplaced);

        return queryAndParams;
    }
}
