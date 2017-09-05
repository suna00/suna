package net.ion.ice.cjmwave.external.pip;

import net.ion.ice.cjmwave.external.utils.CommonNetworkUtils;
import net.ion.ice.cjmwave.external.utils.JSONNetworkUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 5..
 */
@Service
public class PipApiService {

    private Logger logger = Logger.getLogger(PipApiService.class);

    @Value("${cjapi.pip.programurl}")
    String programApiUrl;

    @Value("${cjapi.pip.clipmediaurl}")
    String clipMediaApiUrl;

    public List fetchProgram (String paramStr) throws Exception {
        return JSONNetworkUtils.fetchJSON(programApiUrl, paramStr);
    }

    public List fetchClipMedia (String paramStr) throws Exception {
        return JSONNetworkUtils.fetchJSON(clipMediaApiUrl, paramStr);
    }

    public List fetchProgram (Map paramMap) throws Exception {
        return fetchProgram(CommonNetworkUtils.MapToString(paramMap));
    }

    public List fetchClipMedia (Map paramMap) throws Exception {
        return fetchClipMedia(CommonNetworkUtils.MapToString(paramMap));
    }
};