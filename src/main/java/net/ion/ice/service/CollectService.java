package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("collectService")
public class CollectService {

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService ;

    public ExecuteContext collect(ExecuteContext context){
        newProduct();

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
    public void newProduct() {
        try{
            LocalDateTime now = LocalDateTime.now();

            String[] siteTypes = {"company", "university"};
            for(String siteType : siteTypes){

                List<Node> newProdcuts = nodeService.getNodeList("newProduct", "siteType_matching=" + siteType +"&year_matching=" + now.getYear() + "&month_matching=" + now.getMonthValue() + "&newProductStatus_matching=y");
                Node newProduct = null;

                if(newProdcuts != null && newProdcuts.size() > 0){
                    newProduct = newProdcuts.get(0);
                    List<Node> newProdcutMapList = nodeService.getNodeList("newProductMap", "newProductId_matching=" + newProduct.getId());
                    for(Node newProductMap : newProdcutMapList){
                        nodeService.deleteNode("newProductMap", newProductMap.getId());
                    }
                }else {
                    newProduct = createNewProductData(now, siteType);
                }

                String searchText = "productStatus_matching=y" +
                        "&approvalStatus_matching=approve" +
                        "&deleteStatus_notMatching=approve" +
                        "&saleStatus_matching=sale" +
                        "&sorting=approvalDate desc" +
                        "&siteType_matching=all," + siteType ;


                List<Node> newList = NodeUtils.getNodeList("product", searchText + "&approvalDate_fromto=" + getMonthDays(now));
                Map<String, Object> distinctProducts = new LinkedHashMap<>();

                int i = 0;
                newProductCreate(now, newProduct, newList, distinctProducts, i);

                // - 해당월에 신규 등록된 상품이 없거나  30개 미만일 경우 익월 상품이 노출된다
                if(newList.size() < 30){
                    List<Node> beforeMonthList = nodeService.getNodeList("product", searchText + "&approvalDate_under=" + getLastMonthBefor(now) + "&limit=" + (30 - newList.size()));
                    newProductCreate(now, newProduct, beforeMonthList, distinctProducts, i);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void newProductCreate(LocalDateTime now, Node newProduct, List<Node> newList, Map<String, Object> distinctProducts, int i) {
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

    public String getMonthDays(LocalDateTime dateTime){
        String ym = String.valueOf(dateTime.getYear()).concat(String.valueOf(dateTime.getMonthValue())) ;
        String firstDate = ym + "01";
        String lastDate = ym + (String.valueOf(dateTime.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth()));
        return getFromToString(firstDate, lastDate);
    }

    public String getLastMonthBefor(LocalDateTime dateTime){
        String ym = String.valueOf(dateTime.getYear()).concat(String.valueOf(dateTime.getMonthValue())) ;
        return ym + "01";
    }

    public String getFromToString(String firstDate, String lastDate){
        //20170901~20170930
        return firstDate.concat("~").concat(lastDate);
    }

    //1, 7, 30
    public String getPeriodDate(int day){
        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String before = LocalDate.now().minusDays(day).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return getFromToString(before, now);
    }

    public void bestProduct(){
        try{
//            판매 : purchaseCount	구매건수
//            조회 : viewCount	조회수
//            장바구니
//            위시리스트
            bestSaleBydays(1);
            bestSaleBydays(7);
            bestSaleBydays(30);

            bestViewBydays(30);
            bestCartBydays(30);
            bestWishBydays(30);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void bestSaleBydays(int day) {
        String searchText = "created_fromto="+getPeriodDate(day);
        List<Map<String, Object>> list = nodeBindingService.list("orderProduct", searchText);
        Map<String, Object> distinctProducts = new LinkedHashMap<>();

        int i = 0;
    }

    public void bestViewBydays(int day) {

    }

    public void bestCartBydays(int day) {

    }

    public void bestWishBydays(int day) {

    }


    public void bestViewByCategory(String categoryId) {

    }

    public void bestCartByCategory(String categoryId) {

    }

    public void bestWishByCategory(String categoryId) {

    }


}
