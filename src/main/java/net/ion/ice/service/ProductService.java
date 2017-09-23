package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeQuery;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.DELETE;
import java.io.IOException;
import java.util.*;

@Service("productService")
public class ProductService {
    @Autowired
    private NodeService nodeService ;

    @Autowired
    private NodeBindingService nodeBindingService ;

    public void make(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        try{
            nodeService.executeNode(data, "product", EventService.SAVE);

            List<Node> baseOptions = productOptions(data, "baseOption");
            List<Node> addOptions = productOptions(data, "addOption");
            productOptionItems(data, baseOptions, "baseOption");
            productOptionItems(data, addOptions, "addOption");
            productAttribute(data);
            productToCategoryMap(data);
            productSearchFilter(data);

        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public List<Node> productOptions(Map<String, Object> data, String type) throws IOException {
        List<Node> list = new ArrayList<>();
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get(type).toString());

        if(data.get(type) == null || data.get(type + "Item") == null) return list;

        String searchText = "";
        if("baseOption".equals(type)){
            searchText = "productId_matching="+data.get("productId")+"&productOptionType_notMatching=add";
        }else{
            searchText = "productId_matching="+data.get("productId")+"&productOptionType_matching=add";
        }
        List<Node> nodeList = NodeUtils.getNodeList("productOption", searchText);

        for(Node node : nodeList){
            Boolean exist = false;
            for(Map<String, Object> map : maps){
                String name = map.get("name").toString();
                if(name.equals(node.getValue("name"))){
                    exist = true;
                }
            }
            if(!exist){
                node.put("productOptionStatus", "n");
                nodeService.executeNode(node, "productOption", EventService.UPDATE);
            }
        }

        for(Map<String, Object> map : maps) {

            map = setMap(data, map);
            if (type.equals("baseOption")) {
                map.put("productOptionType", map.get("baseOptionType"));
                map.put("required", "true");
            } else {
                map.put("productOptionType", "add");
            }

            List<Node> nodes = NodeUtils.getNodeList("productOption", "productId_matching="+data.get("productId")+"&name_matching="+map.get("name").toString());
            if(nodes.size() > 0){
                map.put("productOptionId", nodes.get(0).getId());
            }
            Node node = (Node) nodeService.executeNode(map, "productOption", EventService.SAVE);

            list.add(node);
        }
        return list;
    }

    private Map<String, Object>  setMap(Map<String, Object> data, Map<String, Object> map) {
        Map<String, Object> m = new LinkedHashMap<>(data);
        m.putAll(map);
        return m;
    }

    private List<Node> productOptionItems(Map<String, Object> data, List<Node> options, String type) throws IOException {
        List<Node> list = new ArrayList<>();
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get(type + "Item").toString());
        if(data.get(type) == null || data.get(type + "Item") == null) return list;

        String searchText = "";
        if("baseOption".equals(type)){
            searchText = "productId_matching="+data.get("productId")+"&productOptionType_notMatching=add";
        }else{
            searchText = "productId_matching="+data.get("productId")+"&productOptionType_matching=add";
        }

        List<Node> nodeList = NodeUtils.getNodeList("productOptionItem", searchText);
        for(Node node : nodeList){
            Boolean exist = false;
            for(Map<String, Object> map : maps){
                String name = map.get("name").toString();
                if(name.equals(node.getValue("name"))){
                    exist = true;
                }
            }
            if(!exist){
                node.put("productOptionItemStatus", "n");
                nodeService.executeNode(node, "productOptionItem", EventService.UPDATE);
            }
        }

        for(Map<String, Object> item : maps){

            List<Node> nodes = NodeUtils.getNodeList("productOptionItem", "productId_matching="+data.get("productId")+"&productOptionCodeCase_matching="+item.get("productOptionCodeCase").toString());
            if(nodes.size() > 0){
                item.put("productOptionItemId", nodes.get(0).getId());
            }

            item = setMap(data, item);
            item.put("productOptionType", ("baseOption".equals(type) ? options.get(0).get("productOptionType").toString() : "add"));
            item.put("productOptionCodeCase", item.get("productOptionCodeCase").toString());

            Node node = (Node) nodeService.executeNode(item, "productOptionItem", EventService.SAVE);
            list.add(node);
        }

        return list;
    }

    private void productAttribute(Map<String, Object> data) throws IOException {
        if (data.get("productAttributeCategoryId") == null || data.get("productAttribute") == null) return;

        String productId = data.get("productId").toString();
        String productAttributeCategoryId = data.get("productAttributeCategoryId").toString();
        List<Node> existProductAttributeList = (List<Node>) NodeQuery.build("productAttribute").matching("productId", productId).getList();
        List<Map<String, Object>> productAttributeList = JsonUtils.parsingJsonToList(data.get("productAttribute").toString());

        List<Map<String, Object>> saveProductAttributeList = new ArrayList<>();
        List<Node> deleteProductAttributeList = new ArrayList<>();

        for (Map<String, Object> productAttribute : productAttributeList) {
            String productAttributeCategoryItemId = productAttribute.get("productAttributeCategoryItemId").toString();

            Node tempExistProductAttribute = null;
            for (Node existProductAttribute : existProductAttributeList) {
                String existProductAttributeCategoryItemId = existProductAttribute.getStringValue("productAttributeCategoryItemId");
                if (StringUtils.equals(productAttributeCategoryItemId, existProductAttributeCategoryItemId)) {
                    tempExistProductAttribute = existProductAttribute;
                }
            }

            Map<String, Object> saveProductAttribute = new HashMap<>();
            if (tempExistProductAttribute != null) saveProductAttribute.put("productAttributeId", tempExistProductAttribute.getStringValue("productAttributeId"));
            saveProductAttribute.put("productId", productId);
            saveProductAttribute.put("productAttributeCategoryId", productAttributeCategoryId);
            saveProductAttribute.put("productAttributeCategoryItemId", productAttribute.get("productAttributeCategoryItemId"));
            saveProductAttribute.put("name", productAttribute.get("name"));
            saveProductAttribute.put("value", productAttribute.get("value"));

            saveProductAttributeList.add(saveProductAttribute);
        }

        for (Node existProductAttribute : existProductAttributeList) {
            String existProductAttributeCategoryItemId = existProductAttribute.getStringValue("productAttributeCategoryItemId");

            boolean exist = false;
            for (Map<String, Object> productAttribute : productAttributeList) {
                String productAttributeCategoryItemId = productAttribute.get("productAttributeCategoryItemId").toString();
                if (StringUtils.equals(existProductAttributeCategoryItemId, productAttributeCategoryItemId)) exist = true;
            }

            if (!exist) deleteProductAttributeList.add(existProductAttribute);
        }

        for (Map<String, Object> saveProductAttribute : saveProductAttributeList) {
            nodeService.executeNode(saveProductAttribute, "productAttribute", EventService.SAVE);
        }

        for (Node deleteProductAttribute : deleteProductAttributeList) {
            nodeService.executeNode(deleteProductAttribute, "productAttribute", EventService.DELETE);
        }
    }

    private void productToCategoryMap(Map<String, Object> data) throws IOException {
        if (data.get("productToCategoryMap") == null) return;

        String productId = data.get("productId").toString();
        List<Map<String, Object>> existProductToCategoryMapList = (List<Map<String, Object>>) NodeQuery.build("productToCategoryMap").matching("productId", productId).getList();
        List<Map<String, Object>> productToCategoryMapList = JsonUtils.parsingJsonToList(data.get("productToCategoryMap").toString());

        List<Map<String, Object>> saveProductToCategoryMapList = new ArrayList<>();
        List<Map<String, Object>> deleteProductToCategoryMapList = new ArrayList<>();

        for (Map<String, Object> productToCategoryMap : productToCategoryMapList) {
            String categoryId = productToCategoryMap.get("categoryId").toString();

            Map<String, Object> tempExistProductToCategoryMap = null;
            for (Map<String, Object> existProductToCategoryMap : existProductToCategoryMapList) {
                String existCategoryId = existProductToCategoryMap.get("categoryId").toString();
                if (StringUtils.equals(categoryId, existCategoryId)) {
                    tempExistProductToCategoryMap = existProductToCategoryMap;
                }
            }

            Map<String, Object> saveProductToCategoryMap = new HashMap<>();
            if (tempExistProductToCategoryMap != null) saveProductToCategoryMap.put("productToCategoryMapId", tempExistProductToCategoryMap.get("productToCategoryMapId"));
            saveProductToCategoryMap.put("productId", productId);
            saveProductToCategoryMap.put("categoryId", productToCategoryMap.get("categoryId"));

            saveProductToCategoryMapList.add(saveProductToCategoryMap);
        }

        for (Map<String, Object> existProductToCategoryMap : existProductToCategoryMapList) {
            String existCategoryId = existProductToCategoryMap.get("categoryId").toString();

            boolean exist = false;
            for (Map<String, Object> productToCategoryMap : productToCategoryMapList) {
                String categoryId = productToCategoryMap.get("categoryId").toString();
                if (StringUtils.equals(existCategoryId, categoryId)) exist = true;
            }

            if (!exist) deleteProductToCategoryMapList.add(existProductToCategoryMap);
        }

        for (Map<String, Object> saveProductToCategoryMap : saveProductToCategoryMapList) {
            nodeService.executeNode(saveProductToCategoryMap, "productToCategoryMap", EventService.SAVE);
        }

        for (Map<String, Object> deleteProductToCategoryMap : deleteProductToCategoryMapList) {
            nodeService.executeNode(deleteProductToCategoryMap, "productToCategoryMap", EventService.DELETE);
        }
    }

    private void productSearchFilter(Map<String, Object> data) throws IOException {
        if (data.get("productSearchFilter") == null) return;

        String productId = data.get("productId").toString();
        List<Map<String, Object>> existProductSearchFilterList = (List<Map<String, Object>>) NodeQuery.build("productSearchFilter").matching("productId", productId).getList();
        List<Map<String, Object>> productSearchFilterList = JsonUtils.parsingJsonToList(data.get("productSearchFilter").toString());

        List<Map<String, Object>> saveProductSearchFilterList = new ArrayList<>();
        List<Map<String, Object>> deleteProductSearchFilterList = new ArrayList<>();

        for (Map<String, Object> productSearchFilter : productSearchFilterList) {
            String searchFilterId = productSearchFilter.get("searchFilterId").toString();

            Map<String, Object> tempExistProductSearchFilter = null;
            for (Map<String, Object> existProductSearchFilter : existProductSearchFilterList) {
                String existSearchFilterId = existProductSearchFilter.get("searchFilterId").toString();
                if (StringUtils.equals(searchFilterId, existSearchFilterId)) {
                    tempExistProductSearchFilter = existProductSearchFilter;
                }
            }

            Map<String, Object> saveProductSearchFilter = new HashMap<>();
            if (tempExistProductSearchFilter != null) saveProductSearchFilter.put("productSearchFilterId", tempExistProductSearchFilter.get("productSearchFilterId"));
            saveProductSearchFilter.put("productId", productId);
            saveProductSearchFilter.put("searchFilterId", productSearchFilter.get("searchFilterId"));
            saveProductSearchFilter.put("searchFilterCodeIds", productSearchFilter.get("searchFilterCodeIds"));

            saveProductSearchFilterList.add(saveProductSearchFilter);
        }

        for (Map<String, Object> existProductSearchFilter : existProductSearchFilterList) {
            String existSearchFilterId = existProductSearchFilter.get("searchFilterId").toString();

            boolean exist = false;
            for (Map<String, Object> productSearchFilter : productSearchFilterList) {
                String searchFilterId = productSearchFilter.get("searchFilterId").toString();
                if (StringUtils.equals(existSearchFilterId, searchFilterId)) exist = true;
            }

            if (!exist) deleteProductSearchFilterList.add(existProductSearchFilter);
        }

        for (Map<String, Object> saveProductSearchFilter : saveProductSearchFilterList) {
            nodeService.executeNode(saveProductSearchFilter, "productSearchFilter", EventService.SAVE);
        }

        for (Map<String, Object> deleteProductSearchFilter : deleteProductSearchFilterList) {
            nodeService.executeNode(deleteProductSearchFilter, "productSearchFilter", EventService.DELETE);
        }
    }
}
