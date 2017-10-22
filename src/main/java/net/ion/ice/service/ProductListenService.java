package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("productListenService")
public class ProductListenService {

    public static final String PRODUCT_ID = "productId";
    public static final String REVIEW_AVERAGE_SCORE = "reviewAverageScore";
    public static final String REVIEW_COUNT = "reviewCount";

    @Autowired
    private NodeBindingService nodeBindingService ;


    @Autowired
    private NodeService nodeService ;


    public ExecuteContext review(ExecuteContext context){
        Node review = context.getNode() ;
        NodeType reviewType = NodeUtils.getNodeType(review.getTypeId()) ;

        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo(reviewType.getTypeId()).getJdbcTemplate() ;

        Map<String, Object> result = jdbcTemplate.queryForMap("select count(*) as cnt, sum(score) as tot from review where productId = ?", review.get(PRODUCT_ID)) ;

        Node product = NodeUtils.getReferenceNode(review.get(PRODUCT_ID), reviewType.getPropertyType(PRODUCT_ID));
        Integer cnt = (result.get("cnt") == null ? 0 : Integer.parseInt(result.get("cnt").toString())) ;
        product.put(REVIEW_COUNT, result.get("cnt")) ;
        if(cnt == 0){
            product.put(REVIEW_AVERAGE_SCORE, 0) ;
        }else {
            product.put(REVIEW_AVERAGE_SCORE, Double.parseDouble(result.get("tot").toString()) / Double.parseDouble(result.get("cnt").toString()));
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
