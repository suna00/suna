package net.ion.ice.cjmwave.errMsgInfo;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.context.ExecuteContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Created by leehh on 2017. 10. 8.
 */

public class ErrMsgUtil {
    static ErrMsgInfoService errMsgInfoService;

    public ErrMsgUtil(){
        getErrMsgInfoService();
    }
    public static ErrMsgInfoService getErrMsgInfoService(){
        if(errMsgInfoService == null){
            errMsgInfoService = ApplicationContextManager.getBean(ErrMsgInfoService.class);
        }
        return errMsgInfoService;
    }

    public static String getErrMsg(ExecuteContext context, String errCd){
        Map<String, Object> data = context.getData();
        String langCd = null;
        if(data.get("langCd") != null){
            langCd = data.get("langCd").toString();
        }

        try {
            if(StringUtils.isNotEmpty(langCd)){
                return errMsgInfoService.getErrMsg(errCd, langCd);
            }else{
                return errMsgInfoService.getErrMsg(errCd);
            }
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }
}
