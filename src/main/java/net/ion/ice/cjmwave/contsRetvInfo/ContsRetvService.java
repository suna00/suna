//package net.ion.ice.cjmwave.contsRetvInfo;
//
//import com.skb.auth.local.SkbURI;
//import net.ion.ice.IceRuntimeException;
//import net.ion.ice.core.context.ExecuteContext;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.stereotype.Service;
//
//import java.util.Map;
//
//@Service("contsRetvService")
//public class ContsRetvService {
//    public void insert(ExecuteContext context) {
//        Map<String, Object> data = context.getData();
//        if (data == null || StringUtils.isEmpty(data.get("faqSeq").toString())) {
//            throw new IceRuntimeException("faqSeq Parameter is Null") ;
//        }
//    }
//}
