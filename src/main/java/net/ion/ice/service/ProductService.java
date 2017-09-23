package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeQuery;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service("productService")
public class ProductService {
    @Autowired
    private NodeService nodeService ;

    public void make(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        try {
            nodeService.executeNode(data, "product", EventService.SAVE);

            productOption(data);
            productAttribute(data);
            productToCategoryMap(data);
            productSearchFilter(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void productOption(Map<String, Object> data) throws IOException {
        String productId = data.get("productId").toString();
        String baseOptionType = data.get("baseOptionType").toString();
        String baseOption = data.get("baseOption").toString();
        String baseOptionItem = data.get("baseOptionItem").toString();
        String addOptionType = data.get("addOptionType").toString();
        String addOption = data.get("addOption").toString();
        String addOptionItem = data.get("addOptionItem").toString();

        List<Node> existProductOptionList = (List<Node>) NodeQuery.build("baseOption").matching("productId", productId).getList();
        List<Node> existProductOptionItemList = (List<Node>) NodeQuery.build("baseOptionItem").matching("productId", productId).getList();
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
            if (tempExistProductBaseOptionItem != null) saveProductBaseOptionItem.put("productOptionItemId", tempExistProductBaseOptionItem.getStringValue("productOptionItemId"));
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
                if (StringUtils.equals(existProductOptionCodeCase, productOptionCodeCase) && StringUtils.equals(existProductOptionType, baseOptionType)) exist = true;
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
            if (tempExistProductAddOptionItem != null) saveProductAddOptionItem.put("productOptionItemId", tempExistProductAddOptionItem.getStringValue("productOptionItemId"));
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
                if (StringUtils.equals(existProductOptionCodeCase, productOptionCodeCase) && StringUtils.equals(existProductOptionType, addOptionType)) exist = true;
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
