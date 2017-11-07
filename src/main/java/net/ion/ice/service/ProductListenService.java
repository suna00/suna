package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("productListenService")
public class ProductListenService {

    public static final String PRODUCT_ID = "productId";
    public static final String SITE_ID = "siteId";
    public static final String COMPANY_REVIEW_AVERAGE_SCORE = "companyReviewAverageScore";
    public static final String COMPANY_REVIEW_COUNT = "companyReviewCount";
    public static final String UNIVERSITY_REVIEW_AVERAGE_SCORE = "universityReviewAverageScore";
    public static final String UNIVERSITY_REVIEW_COUNT = "universityReviewCount";

    @Autowired
    private NodeBindingService nodeBindingService ;


    @Autowired
    private NodeService nodeService ;


    public ExecuteContext review(ExecuteContext context){
        Node review = context.getNode() ;
        NodeType reviewType = NodeUtils.getNodeType(review.getTypeId()) ;

        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo(reviewType.getTypeId()).getJdbcTemplate() ;

        Map<String, Object> result = jdbcTemplate.queryForMap("select count(*) as cnt, sum(score) as tot from review where productId = ? and siteId = ?", review.get(PRODUCT_ID), review.get(SITE_ID)) ;

        Node product = NodeUtils.getReferenceNode(review.get(PRODUCT_ID), reviewType.getPropertyType(PRODUCT_ID));
        Integer cnt = (result.get("cnt") == null ? 0 : Integer.parseInt(result.get("cnt").toString())) ;

        if(StringUtils.equals("ion",review.getStringValue(SITE_ID))) {
            product.put(COMPANY_REVIEW_COUNT, result.get("cnt"));
            product.put(COMPANY_REVIEW_AVERAGE_SCORE, cnt == 0 ? 0 : (Double.parseDouble(result.get("tot").toString()) / Double.parseDouble(result.get("cnt").toString())));
        } else {
            product.put(UNIVERSITY_REVIEW_COUNT, result.get("cnt"));
            product.put(UNIVERSITY_REVIEW_AVERAGE_SCORE, cnt == 0 ? 0 : (Double.parseDouble(result.get("tot").toString()) / Double.parseDouble(result.get("cnt").toString())));
        }

        nodeService.updateNode(product, "product") ;
        return context ;
    }


    public ExecuteContext purchaseCount(ExecuteContext context){
        String productId = JsonUtils.getStringValue(context.getData(), "productId");
        if(productId == null || productId == "") return context;

        Node product = NodeUtils.getNode("product", productId);
        Integer purchaseCount = product.getIntValue("purchaseCount");
        product.put("purchaseCount", purchaseCount + 1);
        nodeService.updateNode(product, "product") ;

        return context;
    }

    public ExecuteContext viewCount(ExecuteContext context){
        String productId = JsonUtils.getStringValue(context.getData(), "productId");
        if(productId == null || productId == "") return context;

        Node product = NodeUtils.getNode("product", productId);
        Integer viewCount = product.getIntValue("viewCount");
        product.put("viewCount", viewCount + 1);
        nodeService.updateNode(product, "product") ;
        return context;
    }


}
