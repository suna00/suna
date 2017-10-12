package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("newProductService")
public class NewProductService {

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService ;

    public ExecuteContext collect(ExecuteContext context){
        collect();

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("item", "");
        context.setResult(item);
        return context;
    }


    /*
        [신상품 수집기준]
        - 기준: 승인일자 최근순, 기업몰/대학몰 별
        - 집계기간: 수집달
        - 페이지: 신상품
        - 수량: 30개 이상
            - 해당월에 신규 등록된 상품이 없거나  30개 미만일 경우 익월 상품이 노출된다
    "*/


    public void collect() {
        try{
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime before = LocalDateTime.now().minusMonths(1);

            String[] siteTypes = {"company", "university"};
            for(String siteType : siteTypes){
                Node newProduct = createNewProductData(now, siteType);

                String searchText = "productStatus_matching=y" +
                        "&approvalStatus_matching=approve" +
                        "&deleteStatus_notMatching=approve" +
                        "&saleStatus_matching=sale" +
                        "&sorting=approvalDate desc" +
                        "&approvalDate_fromto=";


                List<Node> newList = NodeUtils.getNodeList("product", searchText.concat(getFromToString(now)));
                Map<String, Object> distinctProducts = new LinkedHashMap<>();

                int i = 0;
                for(Node product : newList){
                    Map<String, Object> npMap = new HashMap<>();
                    npMap.put("newProductId", newProduct.getId());
                    npMap.put("productId", product.getId());
                    npMap.put("sortOrder", i++);
                    npMap.put("newProductMapStatus", "y");
                    npMap.put("fixedYn", "n");
                    npMap.put("year", now.getYear());
                    npMap.put("month", now.getMonthValue());
                    nodeService.executeNode(npMap, "newProductMap", CommonService.CREATE);
                    distinctProducts.put(product.getId(), npMap.get("year"));
                }

                if(newList.size() < 30){
                    List<Node> beforeMonthList = nodeService.getNodeList("newProduct", "sorting=newProductId desc&year_matching="+before.getYear()+"&month_matching="+before.getMonthValue());
                    if(beforeMonthList.size() > 0 ){
                        Node beforeMonth = beforeMonthList.get(0);
                        List<Map<String, Object>> beforeMaps = nodeBindingService.list("newProductMap", "sorting=sortOrder&newProductId_equals="+beforeMonth.getId());

                        for(Map<String, Object> map : beforeMaps){
                            if( i <= 30 && distinctProducts.get(JsonUtils.getStringValue(map, "productId")) != null){
                                map.remove("newProductMap");
                                map.put("sortOrder", i++);
                                nodeService.executeNode(map, "newProductMap", CommonService.CREATE);
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Node createNewProductData(LocalDateTime now, String siteType) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", String.valueOf(now.getYear()).concat("년 ").concat(String.valueOf(now.getMonthValue())).concat("월 ").concat(siteType));
        map.put("siteType", siteType);
        map.put("year", now.getYear());
        map.put("month", now.getMonthValue());
        map.put("newProductStatus", "y");
        return (Node) nodeService.executeNode(map, "newProduct", CommonService.CREATE);
    }

    public String getFromToString(LocalDateTime dateTime){
        //20170901~20170930
        String firstDate = String.valueOf(dateTime.getYear()).concat(String.valueOf(dateTime.getMonthValue())).concat(String.valueOf(dateTime.with(TemporalAdjusters.firstDayOfMonth())));
        String lastDate = String.valueOf(dateTime.getYear()).concat(String.valueOf(dateTime.getMonthValue())).concat(String.valueOf(dateTime.with(TemporalAdjusters.lastDayOfMonth())));
        return firstDate.concat("~").concat(lastDate);
    }

}
