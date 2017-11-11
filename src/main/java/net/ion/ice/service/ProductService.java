package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.*;
import net.ion.ice.core.session.SessionService;
import net.ion.ice.plugin.excel.ExcelService;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("productService")
public class ProductService {
    private Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeBindingService nodeBindingService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ExcelService excelService;


    public void make(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        try {
            Node node = (Node) nodeService.executeNode(data, "product", EventService.SAVE);

            productOption(data);
            productAttribute(data);
            productToCategoryMap(data);
            productSearchFilter(data);

            if(context.getChangedProperties().size() > 0){
                List<Node> histories = (List<Node>) NodeQuery.build("productHistory").matching("productId", node.getId()).sorting("version desc").getList();
                Map<String, Object> history = new LinkedHashMap<>();
                history.put("productId", node.getId()) ;
                history.put("version", histories.size() > 0 ? histories.get(0).getIntValue("version") + 1 : 1) ;
                String historyText = "";
                for(String pid : context.getChangedProperties()){
                    historyText += String.format("%s : %s -> %s \r\n", pid, context.getExistNode().getStringValue(pid), node.getStringValue(pid));
                }
                historyText = StringUtils.substringBeforeLast(historyText, "\r\n");
                history.put("history", historyText) ;
                nodeService.createNode(history, "productHistory");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void productOption(Map<String, Object> data) throws IOException {
        String productId = data.get("productId").toString();
        String baseOptionType = data.get("baseOptionType").toString();
        String baseOption = data.get("baseOption").toString();
        String baseOptionItem = data.get("baseOptionItem").toString();
        String addOptionType = "add";
        String addOption = data.get("addOption").toString();
        String addOptionItem = data.get("addOptionItem").toString();

        List<Node> existProductOptionList = (List<Node>) NodeQuery.build("productOption").matching("productId", productId).getList();
        List<Node> existProductOptionItemList = (List<Node>) NodeQuery.build("productOptionItem").matching("productId", productId).getList();
        List<Map<String, Object>> productBaseOptionList = JsonUtils.parsingJsonToList(baseOption);
        List<Map<String, Object>> productBaseOptionItemList = JsonUtils.parsingJsonToList(baseOptionItem);
        List<Map<String, Object>> productAddOptionList = JsonUtils.parsingJsonToList(addOption);
        List<Map<String, Object>> productAddOptionItemList = JsonUtils.parsingJsonToList(addOptionItem);

        List<Map<String, Object>> saveProductBaseOptionList = new ArrayList<>();
        List<Map<String, Object>> saveProductBaseOptionItemList = new ArrayList<>();
        List<Node> deleteProductBaseOptionList = new ArrayList<>();
        List<Node> deleteProductBaseOptionItemList = new ArrayList<>();
        List<Map<String, Object>> saveProductAddOptionList = new ArrayList<>();
        List<Map<String, Object>> saveProductAddOptionItemList = new ArrayList<>();
        List<Node> deleteProductAddOptionList = new ArrayList<>();
        List<Node> deleteProductAddOptionItemList = new ArrayList<>();

        for (Map<String, Object> productBaseOption : productBaseOptionList) {
            String name = productBaseOption.get("name").toString();

            Node tempExistProductBaseOption = null;
            for (Node existProductOption : existProductOptionList) {
                String existName = existProductOption.getStringValue("name");
                String existProductOptionType = existProductOption.getStringValue("productOptionType");

                if (StringUtils.equals(name, existName) && StringUtils.equals(baseOptionType, existProductOptionType)) {
                    tempExistProductBaseOption = existProductOption;
                }
            }

            Map<String, Object> saveProductBaseOption = new HashMap<>();
            if (tempExistProductBaseOption != null) saveProductBaseOption.put("productOptionId", tempExistProductBaseOption.getStringValue("productOptionId"));
            saveProductBaseOption.put("productId", productId);
            saveProductBaseOption.put("productOptionType", baseOptionType);
            saveProductBaseOption.put("name", name);
            saveProductBaseOption.put("required", "true");
            saveProductBaseOption.put("productOptionStatus", "y");
            saveProductBaseOption.put("productOptionCodes", productBaseOption.get("productOptionCodes"));

            saveProductBaseOptionList.add(saveProductBaseOption);
        }

        for (Node existProductOption : existProductOptionList) {
            String existName = existProductOption.getStringValue("name");
            String existProductOptionType = existProductOption.getStringValue("productOptionType");

            boolean exist = false;
            for (Map<String, Object> productBaseOption : productBaseOptionList) {
                String name = productBaseOption.get("name").toString();
                if (StringUtils.equals(existName, name) && StringUtils.equals(existProductOptionType, baseOptionType)) exist = true;
            }

            if (!exist) deleteProductBaseOptionList.add(existProductOption);
        }

        for (Map<String, Object> productBaseOptionItem : productBaseOptionItemList) {
            String productOptionCodeCase = productBaseOptionItem.get("productOptionCodeCase").toString();

            Node tempExistProductBaseOptionItem = null;
            for (Node existProductOptionItem : existProductOptionItemList) {
                String existProductOptionCodeCase = existProductOptionItem.getStringValue("productOptionCodeCase");
                String existProductOptionType = existProductOptionItem.getStringValue("productOptionType");

                if (StringUtils.equals(productOptionCodeCase, existProductOptionCodeCase) && StringUtils.equals(baseOptionType, existProductOptionType)) {
                    tempExistProductBaseOptionItem = existProductOptionItem;
                }
            }

            Map<String, Object> saveProductBaseOptionItem = new HashMap<>();
            if (tempExistProductBaseOptionItem != null)
                saveProductBaseOptionItem.put("productOptionItemId", tempExistProductBaseOptionItem.getStringValue("productOptionItemId"));
            saveProductBaseOptionItem.put("productOptionCodeCase", productOptionCodeCase);
            saveProductBaseOptionItem.put("productId", productId);
            saveProductBaseOptionItem.put("name", productBaseOptionItem.get("name"));
            saveProductBaseOptionItem.put("sortOrder", productBaseOptionItem.get("sortOrder"));
            saveProductBaseOptionItem.put("addPrice", productBaseOptionItem.get("addPrice"));
            saveProductBaseOptionItem.put("supplyPrice", productBaseOptionItem.get("supplyPrice"));
            saveProductBaseOptionItem.put("stockQuantity", productBaseOptionItem.get("stockQuantity"));
            saveProductBaseOptionItem.put("productOptionItemStatus", productBaseOptionItem.get("productOptionItemStatus"));
            saveProductBaseOptionItem.put("productOptionType", baseOptionType);

            saveProductBaseOptionItemList.add(saveProductBaseOptionItem);
        }

        for (Node existProductOptionItem : existProductOptionItemList) {
            String existProductOptionCodeCase = existProductOptionItem.getStringValue("productOptionCodeCase");
            String existProductOptionType = existProductOptionItem.getStringValue("productOptionType");

            boolean exist = false;
            for (Map<String, Object> productBaseOption : productBaseOptionList) {
                String productOptionCodeCase = productBaseOption.get("productOptionCodeCase").toString();
                if (StringUtils.equals(existProductOptionCodeCase, productOptionCodeCase) && StringUtils.equals(existProductOptionType, baseOptionType))
                    exist = true;
            }

            if (!exist) deleteProductBaseOptionItemList.add(existProductOptionItem);
        }

        for (Map<String, Object> productAddOption : productAddOptionList) {
            String name = productAddOption.get("name").toString();

            Node tempExistProductAddOption = null;
            for (Node existProductOption : existProductOptionList) {
                String existName = existProductOption.getStringValue("name");
                String existProductOptionType = existProductOption.getStringValue("productOptionType");

                if (StringUtils.equals(name, existName) && StringUtils.equals(addOptionType, existProductOptionType)) {
                    tempExistProductAddOption = existProductOption;
                }
            }

            Map<String, Object> saveProductAddOption = new HashMap<>();
            if (tempExistProductAddOption != null) saveProductAddOption.put("productOptionId", tempExistProductAddOption.getStringValue("productOptionId"));
            saveProductAddOption.put("productId", productId);
            saveProductAddOption.put("productOptionType", addOptionType);
            saveProductAddOption.put("name", name);
            saveProductAddOption.put("required", productAddOption.get("required"));
            saveProductAddOption.put("productOptionStatus", "y");
            saveProductAddOption.put("productOptionCodes", productAddOption.get("productOptionCodes"));

            saveProductAddOptionList.add(saveProductAddOption);
        }

        for (Node existProductOption : existProductOptionList) {
            String existName = existProductOption.getStringValue("name");
            String existProductOptionType = existProductOption.getStringValue("productOptionType");

            boolean exist = false;
            for (Map<String, Object> productAddOption : productAddOptionList) {
                String name = productAddOption.get("name").toString();
                if (StringUtils.equals(existName, name) && StringUtils.equals(existProductOptionType, addOptionType)) exist = true;
            }

            if (!exist) deleteProductAddOptionList.add(existProductOption);
        }

        for (Map<String, Object> productAddOptionItem : productAddOptionItemList) {
            String productOptionCodeCase = productAddOptionItem.get("productOptionCodeCase").toString();

            Node tempExistProductAddOptionItem = null;
            for (Node existProductOptionItem : existProductOptionItemList) {
                String existProductOptionCodeCase = existProductOptionItem.getStringValue("productOptionCodeCase");
                String existProductOptionType = existProductOptionItem.getStringValue("productOptionType");

                if (StringUtils.equals(productOptionCodeCase, existProductOptionCodeCase) && StringUtils.equals(addOptionType, existProductOptionType)) {
                    tempExistProductAddOptionItem = existProductOptionItem;
                }
            }

            Map<String, Object> saveProductAddOptionItem = new HashMap<>();
            if (tempExistProductAddOptionItem != null)
                saveProductAddOptionItem.put("productOptionItemId", tempExistProductAddOptionItem.getStringValue("productOptionItemId"));
            saveProductAddOptionItem.put("productOptionCodeCase", productOptionCodeCase);
            saveProductAddOptionItem.put("productId", productId);
            saveProductAddOptionItem.put("name", productAddOptionItem.get("name"));
            saveProductAddOptionItem.put("sortOrder", productAddOptionItem.get("sortOrder"));
            saveProductAddOptionItem.put("addPrice", productAddOptionItem.get("addPrice"));
            saveProductAddOptionItem.put("supplyPrice", productAddOptionItem.get("supplyPrice"));
            saveProductAddOptionItem.put("stockQuantity", productAddOptionItem.get("stockQuantity"));
            saveProductAddOptionItem.put("productOptionItemStatus", productAddOptionItem.get("productOptionItemStatus"));
            saveProductAddOptionItem.put("productOptionType", addOptionType);

            saveProductAddOptionItemList.add(saveProductAddOptionItem);
        }

        for (Node existProductOptionItem : existProductOptionItemList) {
            String existProductOptionCodeCase = existProductOptionItem.getStringValue("productOptionCodeCase");
            String existProductOptionType = existProductOptionItem.getStringValue("productOptionType");

            boolean exist = false;
            for (Map<String, Object> productAddOption : productAddOptionList) {
                String productOptionCodeCase = productAddOption.get("productOptionCodeCase").toString();
                if (StringUtils.equals(existProductOptionCodeCase, productOptionCodeCase) && StringUtils.equals(existProductOptionType, addOptionType))
                    exist = true;
            }

            if (!exist) deleteProductAddOptionItemList.add(existProductOptionItem);
        }

        for (Map<String, Object> saveProductBaseOption : saveProductBaseOptionList) {
            nodeService.executeNode(saveProductBaseOption, "productOption", EventService.SAVE);
        }

        for (Map<String, Object> saveProductBaseOptionItem : saveProductBaseOptionItemList) {
            nodeService.executeNode(saveProductBaseOptionItem, "productOptionItem", EventService.SAVE);
        }

        for (Node deleteProductBaseOption : deleteProductBaseOptionList) {
            nodeService.executeNode(deleteProductBaseOption, "productOption", EventService.DELETE);
        }

        for (Node deleteProductBaseOptionItem : deleteProductBaseOptionItemList) {
            nodeService.executeNode(deleteProductBaseOptionItem, "productOptionItem", EventService.DELETE);
        }

        for (Map<String, Object> saveProductAddOption : saveProductAddOptionList) {
            nodeService.executeNode(saveProductAddOption, "productOption", EventService.SAVE);
        }

        for (Map<String, Object> saveProductAddOptionItem : saveProductAddOptionItemList) {
            nodeService.executeNode(saveProductAddOptionItem, "productOptionItem", EventService.SAVE);
        }

        for (Node deleteProductAddOption : deleteProductAddOptionList) {
            nodeService.executeNode(deleteProductAddOption, "productOption", EventService.DELETE);
        }

        for (Node deleteProductAddOptionItem : deleteProductAddOptionItemList) {
            nodeService.executeNode(deleteProductAddOptionItem, "productOptionItem", EventService.DELETE);
        }
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
            if (tempExistProductAttribute != null)
                saveProductAttribute.put("productAttributeId", tempExistProductAttribute.getStringValue("productAttributeId"));
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
            if (tempExistProductToCategoryMap != null)
                saveProductToCategoryMap.put("productToCategoryMapId", tempExistProductToCategoryMap.get("productToCategoryMapId"));
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
            if (tempExistProductSearchFilter != null)
                saveProductSearchFilter.put("productSearchFilterId", tempExistProductSearchFilter.get("productSearchFilterId"));
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

    public void copy(ExecuteContext context) {
        Map<String, Object> paramData = context.getData();
        String productId = paramData.get("productId").toString();
        copyProduct(productId, false, null, null);
    }

    public Node copyProduct(String productId, Boolean isSpecialExhibition, String specialExhibitionId, String specialExhibitionAnkerId) {
        Node productNode = nodeService.read("product", productId);
        Map<String, Object> copyProductData = new HashMap<>();
        NodeType nodeType = nodeService.getNodeType("product");
        List<PropertyType> propertyTypes = new ArrayList<>(nodeType.getPropertyTypes());
        for (PropertyType pt : propertyTypes) {
            if (!StringUtils.equals(pt.getPid(), "productId")) copyProductData.put(pt.getPid(), productNode.get(pt.getPid()));
        }

        if (isSpecialExhibition) {
            copyProductData.put("productType", "special");
            copyProductData.put("upperId", productId);
            copyProductData.put("specialExhibitionId", specialExhibitionId);
            copyProductData.put("specialExhibitionAnkerId", specialExhibitionAnkerId);
        }

        Node copyProductNode = (Node) nodeService.executeNode(copyProductData, "product", EventService.SAVE);
        String copyProductId = copyProductNode.getBindingValue("productId").toString();

        copyProductOption(productId, copyProductId);
        copyProductAttribute(productId, copyProductId);
        copyProductToCategoryMap(productId, copyProductId);
        copyProductSearchFilter(productId, copyProductId);

        return copyProductNode;
    }

    private void copyProductOption(String productId, String newProductId) {
        NodeType productOptionNodeType = nodeService.getNodeType("productOption");
        List<PropertyType> productOptionPropertyTypes = new ArrayList<>(productOptionNodeType.getPropertyTypes());
        NodeType productOptionItemNodeType = nodeService.getNodeType("productOptionItem");
        List<PropertyType> productOptionItemPropertyTypes = new ArrayList<>(productOptionItemNodeType.getPropertyTypes());

        List<Node> productOptionList = (List<Node>) NodeQuery.build("productOption").matching("productId", productId).getList();
        List<Node> productOptionItemList = (List<Node>) NodeQuery.build("productOptionItem").matching("productId", productId).getList();

        List<Map<String, Object>> copyProductOptionDataList = new ArrayList<>();
        for (Node productOptionNode : productOptionList) {
            Map<String, Object> copyProductOptionData = new HashMap<>();
            for (PropertyType pt : productOptionPropertyTypes) {
                copyProductOptionData.put(pt.getPid(), productOptionNode.get(pt.getPid()));
            }
            copyProductOptionData.put("productId", newProductId);
            copyProductOptionDataList.add(copyProductOptionData);
        }

        List<Map<String, Object>> copyProductOptionItemDataList = new ArrayList<>();
        for (Node productOptionItemNode : productOptionItemList) {
            Map<String, Object> copyProductOptionItemData = new HashMap<>();
            for (PropertyType pt : productOptionItemPropertyTypes) {
                copyProductOptionItemData.put(pt.getPid(), productOptionItemNode.get(pt.getPid()));
            }
            copyProductOptionItemData.put("productId", newProductId);
            copyProductOptionItemDataList.add(copyProductOptionItemData);
        }

        for (Map<String, Object> copyProductOptionData : copyProductOptionDataList) {
            nodeService.executeNode(copyProductOptionData, "productOption", EventService.SAVE);
        }

        for (Map<String, Object> copyProductOptionItemData : copyProductOptionItemDataList) {
            nodeService.executeNode(copyProductOptionItemData, "productOptionItem", EventService.SAVE);
        }
    }

    private void copyProductAttribute(String productId, String newProductId) {
        NodeType productAttributeNodeType = nodeService.getNodeType("productAttribute");
        List<PropertyType> productAttributePropertyTypes = new ArrayList<>(productAttributeNodeType.getPropertyTypes());

        List<Node> productAttributeList = (List<Node>) NodeQuery.build("productAttribute").matching("productId", productId).getList();

        List<Map<String, Object>> copyProductAttributeDataList = new ArrayList<>();
        for (Node productAttributeNode : productAttributeList) {
            Map<String, Object> copyProductAttributeData = new HashMap<>();
            for (PropertyType pt : productAttributePropertyTypes) {
                copyProductAttributeData.put(pt.getPid(), productAttributeNode.get(pt.getPid()));
            }
            copyProductAttributeData.put("productId", newProductId);
            copyProductAttributeDataList.add(copyProductAttributeData);
        }

        for (Map<String, Object> copyProductAttributeData : copyProductAttributeDataList) {
            nodeService.executeNode(copyProductAttributeData, "productAttribute", EventService.SAVE);
        }
    }

    private void copyProductToCategoryMap(String productId, String newProductId) {
        NodeType productToCategoryMapNodeType = nodeService.getNodeType("productToCategoryMap");
        List<PropertyType> productToCategoryMapPropertyTypes = new ArrayList<>(productToCategoryMapNodeType.getPropertyTypes());

        List<Map<String, Object>> productToCategoryMapList = (List<Map<String, Object>>) NodeQuery.build("productToCategoryMap").matching("productId", productId).getList();

        List<Map<String, Object>> copyProductToCategoryMapDataList = new ArrayList<>();
        for (Map<String, Object> productToCategoryMap : productToCategoryMapList) {
            Map<String, Object> copyProductToCategoryMapData = new HashMap<>();
            for (PropertyType pt : productToCategoryMapPropertyTypes) {
                copyProductToCategoryMapData.put(pt.getPid(), productToCategoryMap.get(pt.getPid()));
            }
            copyProductToCategoryMapData.put("productId", newProductId);
            copyProductToCategoryMapDataList.add(copyProductToCategoryMapData);
        }

        for (Map<String, Object> copyProductToCategoryMapData : copyProductToCategoryMapDataList) {
            nodeService.executeNode(copyProductToCategoryMapData, "productToCategoryMap", EventService.SAVE);
        }
    }

    private void copyProductSearchFilter(String productId, String newProductId) {
        NodeType productSearchFilterNodeType = nodeService.getNodeType("productSearchFilter");
        List<PropertyType> productSearchFilterPropertyTypes = new ArrayList<>(productSearchFilterNodeType.getPropertyTypes());

        List<Map<String, Object>> productSearchFilterList = (List<Map<String, Object>>) NodeQuery.build("productSearchFilter").matching("productId", productId).getList();

        List<Map<String, Object>> copyProductSearchFilterDataList = new ArrayList<>();
        for (Map<String, Object> productSearchFilter : productSearchFilterList) {
            Map<String, Object> copyProductSearchFilterData = new HashMap<>();
            for (PropertyType pt : productSearchFilterPropertyTypes) {
                copyProductSearchFilterData.put(pt.getPid(), productSearchFilter.get(pt.getPid()));
            }
            copyProductSearchFilterData.put("productId", newProductId);
            copyProductSearchFilterDataList.add(copyProductSearchFilterData);
        }

        for (Map<String, Object> copyProductToCategoryMapData : copyProductSearchFilterDataList) {
            nodeService.executeNode(copyProductToCategoryMapData, "productSearchFilter", EventService.SAVE);
        }
    }

    public void confirmPurchasingHistory(ExecuteContext context) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            Map<String, Object> session = sessionService.getSession(context.getHttpRequest());
            String memberNo = JsonUtils.getStringValue(session, "member.memberNo");
//            Calendar cal = Calendar.getInstance();
//            String now = new SimpleDateFormat("yyyyMMddHHmmss").format(cal.getTime());
//
//            cal.add(Calendar.MONTH, -1);
//
//            String aMonthAgo = new SimpleDateFormat("yyyyMMddHHmmss").format(cal.getTime());

            Map<String, Object> data = context.getData();
            String productId = JsonUtils.getStringValue(data, "productId");

//            List<Map<String, Object>> orderSheetList = nodeBindingService.list("orderSheet", "memberNo_equals=".concat(memberNo).concat("&created_fromto=").concat(aMonthAgo).concat("~").concat(now));
            List<Map<String, Object>> orderSheetList = nodeBindingService.list("orderSheet", "memberNo_equals=".concat(memberNo));

            resultMap.put("isPurchase", false);

            if (orderSheetList.size() > 0) {
                for (Map<String, Object> orderSheet : orderSheetList) {
                    String orderSheetId = JsonUtils.getStringValue(orderSheet, "orderSheetId");
                    /*배송완료, 구매확정, 교환완료*/
                    List<Map<String, Object>> orderProductList = nodeBindingService.list("orderProduct", "orderSheetId_equals=".concat(orderSheetId).concat("&orderStatus_in=order006,order007,order016").concat("&productId_equals=").concat(productId));
                    if(orderProductList.size() > 0){
                        resultMap.put("isPurchase", true);
                        break;
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        context.setResult(resultMap);
    }

    public ExecuteContext downloadExcelForm(ExecuteContext context) {
        HttpServletRequest request = context.getHttpRequest();
        HttpServletResponse response = context.getHttpResponse();

        Map<String, Object> params = context.getData();
        String contentsType = params.get("contentsType") == null ? "" : params.get("contentsType").toString();
        String fileName = "";
        if (StringUtils.equals(contentsType, "goods")) {
            fileName = "일반상품 등록 양식";
        } else if (StringUtils.equals(contentsType, "cellphone")) {
            fileName = "휴대폰상품 등록 양식";
        } else if (StringUtils.equals(contentsType, "healthCheckup")) {
            fileName = "건강검진상품 등록 양식";
        }

        Workbook workbook = null;
        OutputStream outputStream = null;

        try {
            workbook = excelService.getXlsxWorkbook();

            setProductSheet(workbook, contentsType);

            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+excelService.encodingFileName(request, fileName)+"\".xlsx");
            outputStream = response.getOutputStream();
            workbook.write(outputStream);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        return context;
    }

    private void setProductSheet(Workbook workbook, String contentsType) {
        String sheetName = "";
        List<String> cellNameList = null;
        if (StringUtils.equals(contentsType, "goods")) {
            sheetName = "일반상품";
            cellNameList = Arrays.asList(StringUtils.split("상품 타입,벤더,상품명,모델명,MD 코멘트,상품검색 키워드,출시일,과세구분,상품 정보고시,반품/교환정보,재고수량,정상가,할인가,공급가,최소구매수량,최대구매수량", ","));
        } else if (StringUtils.equals(contentsType, "cellphone")) {
            sheetName = "휴대폰상품";
            cellNameList = Arrays.asList(StringUtils.split("상품 타입,벤더,상품명,모델명,MD 코멘트,상품검색 키워드,출시일,과세구분,상품 정보고시,반품/교환정보,재고수량,할인가,신규개통링크,번호이동링크,기기변경링크", ","));
        } else if (StringUtils.equals(contentsType, "healthCheckup")) {
            sheetName = "건강검진상품";
            cellNameList = Arrays.asList(StringUtils.split("상품 타입,벤더,상품명,모델명,MD 코멘트,상품검색 키워드,출시일,과세구분,상품 정보고시,반품/교환정보,재고수량,할인가,건강검진신청 링크 URL", ","));
        }

        Sheet sheet1 = workbook.createSheet(sheetName);
        Row row1 = sheet1.createRow(0);

        for (int i=0; i<cellNameList.size(); i++) {
            Cell cell = row1.createCell(i);
            cell.setCellValue(cellNameList.get(i));
            cell.setCellStyle(excelService.getHeaderCellStyle(workbook));
        }

        for (int i=0; i<cellNameList.size(); i++) {
            sheet1.autoSizeColumn(i);
            sheet1.setColumnWidth(i, sheet1.getColumnWidth(i)+512);
        }

        Sheet sheet2 = workbook.createSheet("상품 타입");
        setCodeSheet(workbook, sheet2, "siteType");

        Sheet sheet3 = workbook.createSheet("벤더");
        setVendorSheet(workbook, sheet3);

        Sheet sheet4 = workbook.createSheet("과세구분");
        setCodeSheet(workbook, sheet4, "taxType");

        Sheet sheet5 = workbook.createSheet("상품 정보고시");
        setProductAttributeCategorySheet(workbook, sheet5);

        Sheet sheet6 = workbook.createSheet("반품 교환정보");
        setReturnExchangePolicySheet(workbook, sheet6);
    }

    private void setVendorSheet(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        Cell headerCell0 = headerRow.createCell(0);
        headerCell0.setCellValue("벤더 코드값");
        headerCell0.setCellStyle(excelService.getHeaderCellStyle(workbook));
        Cell headerCell1 = headerRow.createCell(1);
        headerCell1.setCellValue("벤더 코드명");
        headerCell1.setCellStyle(excelService.getHeaderCellStyle(workbook));

        int rowCount = 1;
        List<Node> affiliateList = (List<Node>) NodeQuery.build("vendor").matching("vendorStatus", "y").sorting("created desc").getList();
        for (Node affiliateNode : affiliateList) {
            Row dataRow = sheet.createRow(rowCount);
            Cell dataCell0 = dataRow.createCell(0);
            dataCell0.setCellValue(affiliateNode.getBindingValue("vendorId").toString());
            Cell dataCell1 = dataRow.createCell(1);
            dataCell1.setCellValue(affiliateNode.getBindingValue("name").toString());

            rowCount++;
        }

        sheet.autoSizeColumn(0);
        sheet.setColumnWidth(0, sheet.getColumnWidth(0)+512);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(1, sheet.getColumnWidth(1)+512);
    }

    private void setProductAttributeCategorySheet(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        Cell headerCell0 = headerRow.createCell(0);
        headerCell0.setCellValue("상품 정보고시 코드값");
        headerCell0.setCellStyle(excelService.getHeaderCellStyle(workbook));
        Cell headerCell1 = headerRow.createCell(1);
        headerCell1.setCellValue("상품 정보고시 코드명");
        headerCell1.setCellStyle(excelService.getHeaderCellStyle(workbook));

        int rowCount = 1;
        List<Node> affiliateList = (List<Node>) NodeQuery.build("productAttributeCategory").matching("productAttributeCategoryStatus", "y").sorting("created desc").getList();
        for (Node affiliateNode : affiliateList) {
            Row dataRow = sheet.createRow(rowCount);
            Cell dataCell0 = dataRow.createCell(0);
            dataCell0.setCellValue(affiliateNode.getBindingValue("productAttributeCategoryId").toString());
            Cell dataCell1 = dataRow.createCell(1);
            dataCell1.setCellValue(affiliateNode.getBindingValue("name").toString());

            rowCount++;
        }

        sheet.autoSizeColumn(0);
        sheet.setColumnWidth(0, sheet.getColumnWidth(0)+512);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(1, sheet.getColumnWidth(1)+512);
    }

    private void setReturnExchangePolicySheet(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        Cell headerCell0 = headerRow.createCell(0);
        headerCell0.setCellValue("반품/교환정보 코드값");
        headerCell0.setCellStyle(excelService.getHeaderCellStyle(workbook));
        Cell headerCell1 = headerRow.createCell(1);
        headerCell1.setCellValue("반품/교환정보 코드명");
        headerCell1.setCellStyle(excelService.getHeaderCellStyle(workbook));

        int rowCount = 1;
        List<Node> affiliateList = (List<Node>) NodeQuery.build("returnExchangePolicy").matching("returnExchangePolicyStatus", "y").sorting("created desc").getList();
        for (Node affiliateNode : affiliateList) {
            Row dataRow = sheet.createRow(rowCount);
            Cell dataCell0 = dataRow.createCell(0);
            dataCell0.setCellValue(affiliateNode.getBindingValue("returnExchangePolicyId").toString());
            Cell dataCell1 = dataRow.createCell(1);
            dataCell1.setCellValue(affiliateNode.getBindingValue("name").toString());

            rowCount++;
        }

        sheet.autoSizeColumn(0);
        sheet.setColumnWidth(0, sheet.getColumnWidth(0)+512);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(1, sheet.getColumnWidth(1)+512);
    }

    private void setCodeSheet(Workbook workbook, Sheet sheet, String upperCode) {
        Row headerRow = sheet.createRow(0);
        Cell headerCell0 = headerRow.createCell(0);
        headerCell0.setCellValue("코드값");
        headerCell0.setCellStyle(excelService.getHeaderCellStyle(workbook));
        Cell headerCell1 = headerRow.createCell(1);
        headerCell1.setCellValue("코드명");
        headerCell1.setCellStyle(excelService.getHeaderCellStyle(workbook));

        int rowCount = 1;
        List<Node> codeList = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", upperCode).matching("commonCodeStatus", "y").sorting("sortOrder asc").getList();
        for (Node codeNode : codeList) {
            Row dataRow = sheet.createRow(rowCount);
            Cell dataCell0 = dataRow.createCell(0);
            dataCell0.setCellValue(codeNode.getBindingValue("code").toString());
            Cell dataCell1 = dataRow.createCell(1);
            dataCell1.setCellValue(codeNode.getBindingValue("name").toString());

            rowCount++;
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    public ExecuteContext uploadExcel(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        MultipartFile file = data.get("excelFile") == null ? null : (MultipartFile) data.get("excelFile");
        String contentsType = data.get("contentsType") == null ? "" : data.get("contentsType").toString();
        String fileName = file.getOriginalFilename();

        Map<String, Object> parsedResult = excelService.parsingExcelFile(file);
        Map<String, Object> saveResult = new HashMap<>();

        if (StringUtils.equals(contentsType, "goods")) {
            saveResult = saveGoodsProduct(parsedResult);
        } else if (StringUtils.equals(contentsType, "cellphone")) {
            saveResult = savePhoneProduct(parsedResult);
        } else if (StringUtils.equals(contentsType, "healthCheckup")) {
            saveResult = saveHealthProduct(parsedResult);
        }

        Map<String, Object> productUploadData = new HashMap<>();
        productUploadData.put("vendorId", "");
        productUploadData.put("contentsType", contentsType);
        productUploadData.put("name", fileName);
        productUploadData.put("file", file);
        productUploadData.put("successCount", saveResult.get("successCount"));
        productUploadData.put("failCount", saveResult.get("failCount"));
        productUploadData.put("failDescription", saveResult.get("failDescription"));

        Node result = (Node) nodeService.executeNode(productUploadData, "productUploadData", EventService.CREATE);
        context.setResult(result);

        return context;
    }

    private Map<String, Object> saveGoodsProduct(Map<String, Object> parsedResult) {
        Iterator<String> parsedResultKeyIterator = parsedResult.keySet().iterator();
        String firstKey = parsedResultKeyIterator.next();

        StringBuffer failDescription = new StringBuffer();
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> creatableItems = new ArrayList<>();

        if (parsedResult.get(firstKey) != null) {
            List<Map<String, String>> memberDataList = (List<Map<String, String>>) parsedResult.get("일반상품");
            int rowIndex = 1;
            for (Map<String, String> memberData : memberDataList) {
                List<String> values = new ArrayList<>(memberData.values());

                String siteType = values.get(0);
                String vendorId = values.get(1);
                String name = values.get(2);
                String modelName = values.get(3);
                String mdComment = values.get(4);
                String keyword = values.get(5);
                String releaseDate = values.get(6);
                String taxType = values.get(7);
                String productAttributeCategoryId = values.get(8);
                String returnExchangePolicyId = values.get(9);
                String stockQuantity = values.get(10);
                String consumerPrice = values.get(11);
                String salePrice = values.get(12);
                String supplyPrice = values.get(13);
                String minOrderQuantity = values.get(14);
                String maxOrderQuantity = values.get(15);

                boolean validation = false;

                if (StringUtils.isEmpty(siteType)) {
                    failDescription.append(String.format("%d 행 1 열 상품 타입 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(siteType)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "siteType").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, siteType)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 1 열 잘못된 상품 타입 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(vendorId)) {
                    failDescription.append(String.format("%d 행 2 열 벤더 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(vendorId)) {
                    Node affiliateNode = nodeService.getNode("affiliate", vendorId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 2 열 잘못된 벤더 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(name)) {
                    failDescription.append(String.format("%d 행 3 열 상품명 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(modelName)) {
                    failDescription.append(String.format("%d 행 4 열 모델명 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(taxType)) {
                    failDescription.append(String.format("%d 행 6 열 상품 타입 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(taxType)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "taxType").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, taxType)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 6 열 잘못된 과세구분 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(productAttributeCategoryId)) {
                    failDescription.append(String.format("%d 행 7 열 상품 정보고시 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(productAttributeCategoryId)) {
                    Node affiliateNode = nodeService.getNode("productAttributeCategory", productAttributeCategoryId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 7 열 잘못된 상품 정보고시 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(returnExchangePolicyId)) {
                    failDescription.append(String.format("%d 행 8 열 반품/교환정보 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(returnExchangePolicyId)) {
                    Node affiliateNode = nodeService.getNode("returnExchangePolicy", returnExchangePolicyId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 8 열 잘못된 반품/교환정보 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(stockQuantity)) {
                    failDescription.append(String.format("%d 행 9 열 재고수량 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(stockQuantity) && !StringUtils.isNumeric(stockQuantity)) {
                    failDescription.append(String.format("%d 행 9 열 재고수량 값이 숫자가 아닙니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(consumerPrice)) {
                    failDescription.append(String.format("%d 행 10 열 정상가 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(consumerPrice) && !StringUtils.isNumeric(consumerPrice)) {
                    failDescription.append(String.format("%d 행 10 열 정상가 값이 숫자가 아닙니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(salePrice)) {
                    failDescription.append(String.format("%d 행 11 열 할인가 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(salePrice) && !StringUtils.isNumeric(salePrice)) {
                    failDescription.append(String.format("%d 행 11 열 할인가 값이 숫자가 아닙니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(supplyPrice)) {
                    failDescription.append(String.format("%d 행 12 열 공급가 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(supplyPrice) && !StringUtils.isNumeric(supplyPrice)) {
                    failDescription.append(String.format("%d 행 12 열 공급가 값이 숫자가 아닙니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(minOrderQuantity)) {
                    failDescription.append(String.format("%d 행 13 열 최소구매수량 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(minOrderQuantity) && !StringUtils.isNumeric(minOrderQuantity)) {
                    failDescription.append(String.format("%d 행 13 열 최소구매수량 값이 숫자가 아닙니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(maxOrderQuantity)) {
                    failDescription.append(String.format("%d 행 14 열 최대구매수량 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(maxOrderQuantity) && !StringUtils.isNumeric(maxOrderQuantity)) {
                    failDescription.append(String.format("%d 행 14 열 최대구매수량 값이 숫자가 아닙니다.\r\n", rowIndex));
                    validation = true;
                }

                if (validation) {
                    failCount++;
                } else {
                    Map<String, Object> createMemberData = new HashMap<>();
                    createMemberData.put("siteType", siteType);
                    createMemberData.put("vendorId", vendorId);
                    createMemberData.put("name", name);
                    createMemberData.put("modelName", modelName);
                    createMemberData.put("mdComment", mdComment);
                    createMemberData.put("keyword", keyword);
                    createMemberData.put("releaseDate", releaseDate);
                    createMemberData.put("taxType", taxType);
                    createMemberData.put("productAttributeCategoryId", productAttributeCategoryId);
                    createMemberData.put("returnExchangePolicyId", returnExchangePolicyId);
                    createMemberData.put("stockQuantity", stockQuantity);
                    createMemberData.put("consumerPrice", consumerPrice);
                    createMemberData.put("salePrice", salePrice);
                    createMemberData.put("supplyPrice", supplyPrice);
                    createMemberData.put("minOrderQuantity", minOrderQuantity);
                    createMemberData.put("maxOrderQuantity", maxOrderQuantity);
                    createMemberData.put("productType", "base");
                    createMemberData.put("contentsType", "goods");
                    createMemberData.put("approvalStatus", "request");
                    createMemberData.put("productStatus", "n");
                    createMemberData.put("saleStatus", "stop");

                    creatableItems.add(createMemberData);

                    successCount++;
                }

                rowIndex++;
            }
        }

        if (failCount == 0) {
            for (Map<String, Object> creatableItem : creatableItems) {
                nodeService.executeNode(creatableItem, "product", EventService.CREATE);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failDescription", failDescription.toString());

        return result;
    }

    private Map<String, Object> savePhoneProduct(Map<String, Object> parsedResult) {
        Iterator<String> parsedResultKeyIterator = parsedResult.keySet().iterator();
        String firstKey = parsedResultKeyIterator.next();

        StringBuffer failDescription = new StringBuffer();
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> creatableItems = new ArrayList<>();

        if (parsedResult.get(firstKey) != null) {
            List<Map<String, String>> memberDataList = (List<Map<String, String>>) parsedResult.get("일반상품");
            int rowIndex = 1;
            for (Map<String, String> memberData : memberDataList) {
                List<String> values = new ArrayList<>(memberData.values());

                String siteType = values.get(0);
                String vendorId = values.get(1);
                String name = values.get(2);
                String modelName = values.get(3);
                String mdComment = values.get(4);
                String keyword = values.get(5);
                String releaseDate = values.get(6);
                String taxType = values.get(7);
                String productAttributeCategoryId = values.get(8);
                String returnExchangePolicyId = values.get(9);
                String stockQuantity = values.get(10);
                String salePrice = values.get(11);
                String cellphoneLinkForNew = values.get(12);
                String cellphoneLinkForMove = values.get(13);
                String cellphoneLinkForChange = values.get(14);

                boolean validation = false;

                if (StringUtils.isEmpty(siteType)) {
                    failDescription.append(String.format("%d 행 1 열 상품 타입 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(siteType)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "siteType").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, siteType)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 1 열 잘못된 상품 타입 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(vendorId)) {
                    failDescription.append(String.format("%d 행 2 열 벤더 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(vendorId)) {
                    Node affiliateNode = nodeService.getNode("affiliate", vendorId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 2 열 잘못된 벤더 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(name)) {
                    failDescription.append(String.format("%d 행 3 열 상품명 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(modelName)) {
                    failDescription.append(String.format("%d 행 4 열 모델명 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(taxType)) {
                    failDescription.append(String.format("%d 행 6 열 상품 타입 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(taxType)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "taxType").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, taxType)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 6 열 잘못된 과세구분 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(productAttributeCategoryId)) {
                    failDescription.append(String.format("%d 행 7 열 상품 정보고시 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(productAttributeCategoryId)) {
                    Node affiliateNode = nodeService.getNode("productAttributeCategory", productAttributeCategoryId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 7 열 잘못된 상품 정보고시 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(returnExchangePolicyId)) {
                    failDescription.append(String.format("%d 행 8 열 반품/교환정보 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(returnExchangePolicyId)) {
                    Node affiliateNode = nodeService.getNode("returnExchangePolicy", returnExchangePolicyId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 8 열 잘못된 반품/교환정보 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(stockQuantity)) {
                    failDescription.append(String.format("%d 행 9 열 재고수량 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(stockQuantity) && !StringUtils.isNumeric(stockQuantity)) {
                    failDescription.append(String.format("%d 행 9 열 재고수량 값이 숫자가 아닙니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(salePrice)) {
                    failDescription.append(String.format("%d 행 11 열 할인가 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(salePrice) && !StringUtils.isNumeric(salePrice)) {
                    failDescription.append(String.format("%d 행 11 열 할인가 값이 숫자가 아닙니다.\r\n", rowIndex));
                    validation = true;
                }

                if (validation) {
                    failCount++;
                } else {
                    Map<String, Object> createMemberData = new HashMap<>();
                    createMemberData.put("siteType", siteType);
                    createMemberData.put("vendorId", vendorId);
                    createMemberData.put("name", name);
                    createMemberData.put("modelName", modelName);
                    createMemberData.put("mdComment", mdComment);
                    createMemberData.put("keyword", keyword);
                    createMemberData.put("releaseDate", releaseDate);
                    createMemberData.put("taxType", taxType);
                    createMemberData.put("productAttributeCategoryId", productAttributeCategoryId);
                    createMemberData.put("returnExchangePolicyId", returnExchangePolicyId);
                    createMemberData.put("stockQuantity", stockQuantity);
                    createMemberData.put("salePrice", salePrice);
                    createMemberData.put("cellphoneLinkForNew", cellphoneLinkForNew);
                    createMemberData.put("cellphoneLinkForMove", cellphoneLinkForMove);
                    createMemberData.put("cellphoneLinkForChange", cellphoneLinkForChange);
                    createMemberData.put("productType", "base");
                    createMemberData.put("contentsType", "cellphone");
                    createMemberData.put("approvalStatus", "request");
                    createMemberData.put("productStatus", "n");
                    createMemberData.put("saleStatus", "stop");

                    creatableItems.add(createMemberData);

                    successCount++;
                }

                rowIndex++;
            }
        }

        if (failCount == 0) {
            for (Map<String, Object> creatableItem : creatableItems) {
                nodeService.executeNode(creatableItem, "product", EventService.CREATE);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failDescription", failDescription.toString());

        return result;
    }

    private Map<String, Object> saveHealthProduct(Map<String, Object> parsedResult) {
        Iterator<String> parsedResultKeyIterator = parsedResult.keySet().iterator();
        String firstKey = parsedResultKeyIterator.next();

        StringBuffer failDescription = new StringBuffer();
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> creatableItems = new ArrayList<>();

        if (parsedResult.get(firstKey) != null) {
            List<Map<String, String>> memberDataList = (List<Map<String, String>>) parsedResult.get("일반상품");
            int rowIndex = 1;
            for (Map<String, String> memberData : memberDataList) {
                List<String> values = new ArrayList<>(memberData.values());

                String siteType = values.get(0);
                String vendorId = values.get(1);
                String name = values.get(2);
                String modelName = values.get(3);
                String mdComment = values.get(4);
                String keyword = values.get(5);
                String releaseDate = values.get(6);
                String taxType = values.get(7);
                String productAttributeCategoryId = values.get(8);
                String returnExchangePolicyId = values.get(9);
                String stockQuantity = values.get(10);
                String salePrice = values.get(11);
                String healthCheckupLink = values.get(12);

                boolean validation = false;

                if (StringUtils.isEmpty(siteType)) {
                    failDescription.append(String.format("%d 행 1 열 상품 타입 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(siteType)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "siteType").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, siteType)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 1 열 잘못된 상품 타입 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(vendorId)) {
                    failDescription.append(String.format("%d 행 2 열 벤더 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(vendorId)) {
                    Node affiliateNode = nodeService.getNode("affiliate", vendorId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 2 열 잘못된 벤더 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(name)) {
                    failDescription.append(String.format("%d 행 3 열 상품명 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(modelName)) {
                    failDescription.append(String.format("%d 행 4 열 모델명 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(taxType)) {
                    failDescription.append(String.format("%d 행 6 열 상품 타입 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(taxType)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "taxType").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, taxType)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 6 열 잘못된 과세구분 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(productAttributeCategoryId)) {
                    failDescription.append(String.format("%d 행 7 열 상품 정보고시 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(productAttributeCategoryId)) {
                    Node affiliateNode = nodeService.getNode("productAttributeCategory", productAttributeCategoryId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 7 열 잘못된 상품 정보고시 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(returnExchangePolicyId)) {
                    failDescription.append(String.format("%d 행 8 열 반품/교환정보 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(returnExchangePolicyId)) {
                    Node affiliateNode = nodeService.getNode("returnExchangePolicy", returnExchangePolicyId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 8 열 잘못된 반품/교환정보 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(stockQuantity)) {
                    failDescription.append(String.format("%d 행 9 열 재고수량 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(stockQuantity) && !StringUtils.isNumeric(stockQuantity)) {
                    failDescription.append(String.format("%d 행 9 열 재고수량 값이 숫자가 아닙니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(salePrice)) {
                    failDescription.append(String.format("%d 행 11 열 할인가 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(salePrice) && !StringUtils.isNumeric(salePrice)) {
                    failDescription.append(String.format("%d 행 11 열 할인가 값이 숫자가 아닙니다.\r\n", rowIndex));
                    validation = true;
                }

                if (validation) {
                    failCount++;
                } else {
                    Map<String, Object> createMemberData = new HashMap<>();
                    createMemberData.put("siteType", siteType);
                    createMemberData.put("vendorId", vendorId);
                    createMemberData.put("name", name);
                    createMemberData.put("modelName", modelName);
                    createMemberData.put("mdComment", mdComment);
                    createMemberData.put("keyword", keyword);
                    createMemberData.put("releaseDate", releaseDate);
                    createMemberData.put("taxType", taxType);
                    createMemberData.put("productAttributeCategoryId", productAttributeCategoryId);
                    createMemberData.put("returnExchangePolicyId", returnExchangePolicyId);
                    createMemberData.put("stockQuantity", stockQuantity);
                    createMemberData.put("salePrice", salePrice);
                    createMemberData.put("healthCheckupLink", healthCheckupLink);
                    createMemberData.put("productType", "base");
                    createMemberData.put("contentsType", "healthCheckup");
                    createMemberData.put("approvalStatus", "request");
                    createMemberData.put("productStatus", "n");
                    createMemberData.put("saleStatus", "stop");

                    creatableItems.add(createMemberData);

                    successCount++;
                }

                rowIndex++;
            }
        }

        if (failCount == 0) {
            for (Map<String, Object> creatableItem : creatableItems) {
                nodeService.executeNode(creatableItem, "product", EventService.CREATE);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failDescription", failDescription.toString());

        return result;
    }
}
