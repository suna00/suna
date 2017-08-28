package net.ion.ice.cjmwave.migration;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by juneyoungoh on 2017. 8. 25..
 * 이 서비스는 MSSQL 에 접속할 수 있다
 * 이 서비스는 노드를 생성한다
 */
public abstract class MSSQLMigrationService {

    // 일단은 이게 MSSQL 접속 템플릿이라고 하고
    @Autowired
    JdbcTemplate template;

    @Autowired
    NodeService nodeService;

    /*
    * 메소드 선언 시 1 번 파라미터는 대상 테이블명, 2 번 파라미터는 콜리에서 정의하도록 한다
    * Future??
    * 일단은 생각하지 말고 // 코딩하고 리팩토링 하자
    * */

    /*
    * MT_ARTIST
mt_artist_country
MT_ARTIST_GENRE
MT_ARTIST_KEYWORD
    *
    * */

    private List<Node> convertToNodeList(String nodeTypeId, List<Map<String, Object>> dataList) {
        List<Node> nodeList = new ArrayList<>();
        dataList.forEach(mem -> nodeList.add(convertToNode(nodeTypeId, mem)));
        return nodeList;
    }

    private Map<String, String[]> toStringArrMap(Map<String, Object> originalMap) {
        Map<String, String[]> strMap = new HashMap<String, String[]>();
        originalMap.forEach((k,v) -> {
            String [] strArr = new String[1];
            strArr[0] = String.valueOf(v);
            strMap.put(k, strArr);
        });
        return strMap;
    }

    private Node convertToNode (String nodeTypeId, Map<String, Object> data) {
        Node newNode = new Node();
        data.forEach((k, v) -> {
            newNode.put(k, v);
        });
        return newNode;
    }

    public void migration(){
        Date from = new Date(); // 뭐 최근 30분이라든가...  없으면 쿼리에서 날짜 조건 제외
        String tableName = "artist";
        String targetArtistTbl = "MT_ARTIST";
        String targetArtistCtryTbl = "mt_artist_country";
        String targetArtistGenreTbl = "MT_ARTIST_GENRE";
        String targetArtistKeywordsTbl = "MT_ARTIST_KEYWORD";


        // 어떤 작업이 있을까..
        // 1. 마이그레이션을 대상을 추출하는 쿼리를 파일로 관리하는게 좋겠다
        // 2. 쿼리한 데이터가 맵이면 그대로 밀어넣을 수가 있나?
        // 쿼리 결과가 리스트 맵이면 맵 to Node List
        String query = "SELECT * FROM MT_ARTIST";
        List<Map<String, Object>> rsList = template.queryForList(query);
        String typeId = "artist", event = "save";
        if(rsList != null && !rsList.isEmpty()) {

            rsList.forEach(mem -> {
                //map 이 string, string[] 이네...
                nodeService.executeNode(toStringArrMap(mem)
                        , null, typeId, event);
            } );
        }


    }


    public void doVoid (HttpServletRequest request) {
    }

}
