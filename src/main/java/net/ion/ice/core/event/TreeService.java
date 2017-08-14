package net.ion.ice.core.event;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.Code;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.query.SimpleQueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.stagemonitor.util.StringUtils;

import java.util.*;

/**
 * Created by jaeho on 2017. 7. 11..
 */
@Service("treeService")
public class TreeService {

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";

    public static final String EVENT = "event";

    public static final String EVENT_ACTION = "eventAction";

    public static final String TYPEID = "contsCtgry";


    @Autowired
    private NodeService nodeService ;

    @Autowired
    private InfinispanRepositoryService infinispanService ;

    @Autowired
    private EventBroker eventBroker ;

    public void sortEvent(ExecuteContext context){

        Map<String, Object> _data = context.getData();

        String dragKey = _data.get("dragKey").toString();
        String dropKey = _data.get("dropKey").toString();
        String dropGap = _data.get("dropGap").toString();

        Node dragNode = nodeService.read(TYPEID, dragKey);
        Node dropNode = nodeService.read(TYPEID, dropKey);

        Code dropUpCode = (Code) dropNode.getValue("upperContsCtgryId");
        String dropUpId = ( dropGap.equals("true") ) ? dropUpCode.getValue().toString() : dropNode.getId().toString();

        List<Node> upNodeList = nodeService.getNodeList(TYPEID, "upperContsCtgryId_matching="+dropUpId);

        if(upNodeList.size()>0){

            if(dropGap.equals("false")){
                Collections.sort(upNodeList,new CompareSeqDesc());  //int로 내림차순
                upNodeList.add(dragNode);
            }else{

                Code dragUpCode = (Code) dragNode.getValue("upperContsCtgryId");
                String dragUpId = dragUpCode.getValue().toString();

                if(dropUpId.equals(dragUpId)){
                    upNodeList.remove(dragNode);
                }
                Collections.sort(upNodeList,new CompareSeqAsc()); //int로 오름차순
                int idx = 0;
                int dropOrder = Integer.parseInt(dropNode.getValue("sortOdrg").toString());
                for(int i=0; i<upNodeList.size(); i++){
                    Node node = upNodeList.get(i);
                    int nodeOrder = Integer.parseInt(node.getValue("sortOdrg").toString());
                    if(nodeOrder == dropOrder){
                        idx = i;
                        break;
                    }
                }
                upNodeList.add(idx,dragNode);
            }

            //새정렬 nodelist for문 돌면서 전부 인덱스값으로 sortOdrg 계속 put한다.
            List<Node> newNodeList = upNodeList;
            for(int j=1; j<=newNodeList.size(); j++){
                Node newNode = newNodeList.get(j);

                Map<String, String[]> maps = new HashMap<>();
                maps.put("contsCtgryId",new String[]{newNode.getId()});
                maps.put("upperContsCtgryId",new String[]{dropUpId});
                maps.put("sortOdrg",new String[]{ String.valueOf(j)} );
                nodeService.executeNode(maps,null,TYPEID,"save");
            }

        }else if(dropGap.equals("false") && upNodeList.size() <=0){

            Map<String, String[]> maps = new HashMap<>();
            maps.put("contsCtgryId",new String[]{dragNode.getId()});
            maps.put("upperContsCtgryId",new String[]{dropUpId});
            maps.put("sortOdrg",new String[]{ String.valueOf(1)} );
            nodeService.executeNode(maps,null,TYPEID,"save");
        }

    }

    public SimpleQueryResult getNodeTree(String typeId, Map<String, String[]> parameterMap) {
        return nodeService.getNodeTree(typeId, parameterMap) ;
    }


    /**
     * int로 내림차순(Desc) 정렬
     * @author Administrator
     *
     */
    static class CompareSeqDesc implements Comparator<Node>{
        @Override
        public int compare(Node o1, Node o2) {
            // TODO Auto-generated method stub
            int o1Order = Integer.parseInt(o1.getValue("sortOdrg").toString());
            int o2Order = Integer.parseInt(o2.getValue("sortOdrg").toString());
            return o1Order > o2Order ? -1 : o1Order < o2Order ? 1:0;
        }
    }

    /**
     * int로 오름차순(Asc) 정렬
     * @author Administrator
     *
     */
    static class CompareSeqAsc implements Comparator<Node>{
        @Override
        public int compare(Node o1, Node o2) {
            // TODO Auto-generated method stub
            int o1Order = Integer.parseInt(o1.getValue("sortOdrg").toString());
            int o2Order = Integer.parseInt(o2.getValue("sortOdrg").toString());
            return o1Order < o2Order? -1 : o1Order > o2Order ? 1:0;
        }
    }

}


