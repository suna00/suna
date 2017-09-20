package net.ion.ice.cjmwave.external.pip;

import net.ion.ice.core.data.DBService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 20..
 * 매칭되는 정보가 없으면 무조건 영어 기준으로 내려준다.
 */
@Component
public class ISO639Storage {

    private Map<String, String> alpha3Map;  // 키가 alpha3 규약 3자리
    private Map<String, String> alpha2Map;  // 키가 alpha2 규약 2자리
    private Logger logger;
    private JdbcTemplate ice2Template;

    @Autowired
    DBService dbService;

    @PostConstruct
    public void init(){
        logger = Logger.getLogger(ISO639Storage.class);
        try{
            initializeLanguageInfo();
        } catch (Exception e) {
            logger.info("FAILED TO LOAD ISO639 INFORMATION :: It will bad effect on Migration :-(");
        }
    }

    private void initializeLanguageInfo () {
        if(ice2Template == null) {
            ice2Template = dbService.getJdbcTemplate("cjDb");
        }

        if(alpha2Map == null) alpha2Map = new HashMap<>();
        if(alpha3Map == null) alpha3Map = new HashMap<>();

        String query = "SELECT * FROM LANG_ISO639";
        List<Map<String, Object>> langInfo = ice2Template.queryForList(query);
        for(Map<String, Object> iso639Lang : langInfo) {
            String alpha2Value = iso639Lang.get("ALPHA2").toString();
            String alpha3Value = iso639Lang.get("ALPHA3").toString();
            alpha2Map.put(alpha2Value, alpha3Value);
            alpha3Map.put(alpha3Value, alpha2Value);
        }
        logger.info("ISO639 READY. It is good for Migration! :-)");
    }

    public String getValue(String key){
        if(key.length() == 3) {
            return alpha2Map.get(key.toLowerCase());
        } else if (key.length() == 2) {
            return alpha3Map.get(key.toLowerCase());
        } else {
            return null;
        }
    }
}
