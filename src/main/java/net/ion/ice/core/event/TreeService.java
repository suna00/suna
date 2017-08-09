package net.ion.ice.core.event;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.stagemonitor.util.StringUtils;

import java.util.*;

/**
 * Created by jaeho on 2017. 7. 11..
 */
@Service("treeService")
public class TreeService {

   /* public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";*/

    public static final String EVENT = "event";

    public static final String EVENT_ACTION = "eventAction";


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

        Node dragNode = nodeService.read("contsCtgry", dragKey);
        Node gropNode = nodeService.read("contsCtgry", dropKey);

        int dropOrder = Integer.parseInt(gropNode.getValue("sortOdrg").toString());

        String upperId = ( dropGap.equals("true") ) ? gropNode.getValue("upperContsCtgryId").toString() : gropNode.getId().toString();
        List<Node> upNodeList = nodeService.getNodeList("contsCtgry", "upperContsCtgryId_matching="+upperId);

        if(upNodeList.size()>0){
            dragNode.put("upperContsCtgryId",upperId);

            if(dropGap.equals("false")){
                Collections.sort(upNodeList,new CompareSeqDesc());  //int로 내림차순
                upNodeList.add(dragNode);
            }else{
                if(upperId.equals(dragNode.getValue("upperContsCtgryId").toString())){
                    upNodeList.remove(dragNode);
                }
                Collections.sort(upNodeList,new CompareSeqAsc()); //int로 오름차순
                int idx = 0;
                for(int i=0; i<upNodeList.size(); i++){
                    Node node = upNodeList.get(i);
                    int nodeOrder = Integer.parseInt(node.getValue("sortOdrg").toString());
                    if(nodeOrder == dropOrder){
                        idx = i;
                        break;
                    }
                }
                upNodeList.add(idx,dragNode);   //내가 중간에 들어가고 나머지가 뒤로 밀리는게 맞겠지??
            }

            //새 upNodeList for문 돌면서 전부 인덱스값으로 sortOdrg 계속 put한다
            for(int j=1; j<=upNodeList.size(); j++){
                upNodeList.get(j).put("sortOdrg",j);
            }

        }else if(dropGap.equals("false") && upNodeList.size() <=0){
            dragNode.put("upperContsCtgryId",upperId);
            dragNode.put("sortOdrg",1);
        }

        changeEvent(dragNode);

    }

    private Node changeEvent(Node node) {
        Node dragNode = node;
        return dragNode;
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


