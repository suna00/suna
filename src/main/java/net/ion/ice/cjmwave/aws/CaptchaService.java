package net.ion.ice.cjmwave.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.tomcatsessionmanager.amazonaws.services.dynamodb.sessionmanager.converters.SessionConversionException;
import com.amazonaws.util.IOUtils;

import java.io.*;
import java.util.*;

import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.util.CustomObjectInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by leehh on 2017. 11. 11.
 */
@Service("captchaService")
public class CaptchaService {
    private static Logger logger = LoggerFactory.getLogger(CaptchaService.class);

    @Value("${amazon.dynamodb.endpoint}")
    private String amazonDynamoDBEndpoint;

    private String amazonAWSAccessKey;

    private String amazonAWSSecretKey;

    private String regionId = "ap-northeast-2";



    private DynamoDBMapper dynamoDBMapper;

    private AmazonDynamoDBClient amazonDynamoDB;

    private List<String> allowDomainList = Arrays.asList("internal-g-p-api-Internal-elb-1137130812.ap-northeast-2.elb.amazonaws.com","G-P-Api-ELB-321213426.ap-northeast-2.elb.amazonaws.com","mapi.mwave.me","Test-Api-ELB-334897221.ap-northeast-2.elb.amazonaws.com");

    @PostConstruct
    public void initDynamoMapper(){
        this.amazonDynamoDB =  createDynamoClient();
        this.dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride("MWAVE_SESSION")));
    }

    /**
     * 캡차 입력값 검증
     *
     * @param sessionKey   세션키
     * @param captchaValue 검증요청 값
     * @return 일치 여부
     */

    public Boolean validate(String sessionKey, String captchaValue) {
        String captchaText = (String) RequestContextHolder.getRequestAttributes().getAttribute(sessionKey, RequestAttributes.SCOPE_SESSION);

        return StringUtils.equalsIgnoreCase(captchaText, captchaValue);
    }

    public Boolean validate(HttpServletRequest httpRequest) {
        logger.info("CAPTCHA : " + httpRequest.getServerName()+" | "+httpRequest.getRequestURL().toString());
        String reqServerName = httpRequest.getServerName();
        String reqUrl = StringUtils.substringAfterLast(httpRequest.getRequestURL().toString(), "/");
        //허용된 도메인에서 호출된 경우인지 확인
        if(!allowDomainList.contains(reqServerName)){
            logger.info("serverName validate : serverName="+reqServerName);
            return false;
        }
        Enumeration he = httpRequest.getHeaderNames();
        while (he.hasMoreElements()) {
            String name = (String) he.nextElement();
            logger.info("HEADER : " + name + "=" + httpRequest.getHeader(name) );
        }

        Cookie[] cookies = httpRequest.getCookies();
        if(cookies != null){
            for(Cookie cookie : cookies){
                logger.info("COOKIE : " + cookie.getName() + "=" + cookie.getValue());
//                if(cookie.getName().equals("sessionId")){
//                    sessionKey= cookie.getValue();
//                    break;
//                }
            }
        }

        String vd = httpRequest.getParameter("vd");
        logger.info("VD : " + vd);
        //APP에서 호출한 경우가 아니면, vd는 꼭 넘어와야함
        if(!"mapi.mwave.me".equals(reqServerName) && StringUtils.isEmpty(vd)){
            logger.info("vd validate : serverName="+reqServerName+", vd="+vd);
            return false;
        }

        String sessionKey = httpRequest.getParameter("uuid");
        logger.info("uuid : " + sessionKey);

        if(StringUtils.isNotEmpty(sessionKey)) {
            ConcurrentMap<String, Object> attributes = getSession(sessionKey);

            for(String name : attributes.keySet()) {
                logger.info("SESSION : " + name + "=" + attributes.get(name));
            }

            String itemKey = "";
            if("IfUser002".equals(reqUrl)){
                itemKey = (String) attributes.get("mbrCaptcha_CAPTCHA");
            }else{
                itemKey = (String) attributes.get("voteCaptcha_CAPTCHA");
            }
            logger.info("reqUrl="+reqUrl+", itemKey="+itemKey);
            if(StringUtils.isNotEmpty(itemKey)){
                boolean captchaVaild = validate(itemKey,vd);
                logger.info("captchaValid="+captchaVaild);
                return captchaVaild;
            }
        }


        return true;
    }
    /**
     * 캡차 reset
     *
     * @param sessionKey 세션키
     * @return 일치 여부
     */

    public void reset(String sessionKey) {
        RequestContextHolder.getRequestAttributes().removeAttribute(sessionKey, RequestAttributes.SCOPE_SESSION);
    }

    public void checkSession(String sessionKey){
        ConcurrentMap<String, Object> attributes = getSession(sessionKey);

        for(String name : attributes.keySet()) {
            logger.info("SESSION : " + name + "=" + attributes.get(name));
        }
    }


    public ConcurrentMap<String, Object> getSession(String sessionKey){
        DynamoSessionItem sessionItem = dynamoDBMapper.load(new DynamoSessionItem(sessionKey));
        if (sessionItem != null) {
            return toSession(sessionItem);
        } else {
            return null;
        }

    }

    public ConcurrentMap<String, Object> toSession(DynamoSessionItem sessionItem) {
        ObjectInputStream ois = null;
        try {
            ByteArrayInputStream fis = new ByteArrayInputStream(sessionItem.getSessionData().array());
            ois = new CustomObjectInputStream(fis, this.getClass().getClassLoader());
            return doReadObject(ois);

        } catch (Exception e) {
            throw new SessionConversionException("Unable to convert Dynamo storage representation to a Tomcat Session",
                    e);
        } finally {
            IOUtils.closeQuietly(ois, null);
        }
    }

    protected ConcurrentMap<String, Object> doReadObject(ObjectInputStream stream)
            throws ClassNotFoundException, IOException {
        long creationTime = ((Long) stream.readObject()).longValue();
        long lastAccessedTime = ((Long) stream.readObject()).longValue();
        long maxInactiveInterval = ((Integer) stream.readObject()).intValue();
        boolean isNew = ((Boolean) stream.readObject()).booleanValue();
        boolean isValid = ((Boolean) stream.readObject()).booleanValue();
        long thisAccessedTime = ((Long) stream.readObject()).longValue();
        Object principal = null;        // Transient only
        //        setId((String) stream.readObject());
        String id = (String) stream.readObject();
        // Deserialize the attribute count and attribute values
        ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();
        int n = ((Integer) stream.readObject()).intValue();
        boolean isValidSave = isValid;
        isValid = true;
        for (int i = 0; i < n; i++) {
            String name = (String) stream.readObject();
            final Object value;
            try {
                value = stream.readObject();
            } catch (WriteAbortedException wae) {
                if (wae.getCause() instanceof NotSerializableException) {
                    wae.printStackTrace();
                    continue;
                }
                throw wae;
            }

            attributes.put(name, value);
        }
        return attributes;
    }


    public AmazonDynamoDBClient createDynamoClient() {
        AmazonDynamoDBClient amazonDynamoDB = new AmazonDynamoDBClient(amazonAWSCredentials());

        if (this.regionId != null) {
            amazonDynamoDB.setRegion(RegionUtils.getRegion(this.regionId));
        }

        if (!StringUtils.isEmpty(amazonDynamoDBEndpoint)) {
            amazonDynamoDB.setEndpoint(amazonDynamoDBEndpoint);
        }

        return amazonDynamoDB;
    }

    public AWSCredentialsProvider amazonAWSCredentials() {
//        if(StringUtils.isNotEmpty(this.amazonAWSAccessKey)){
//            return (AWSCredentialsProvider) new StaticCredentialsProvider(new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey));
//        }
        AWSCredentialsProvider defaultChainProvider = new DefaultAWSCredentialsProviderChain();
        if (defaultChainProvider.getCredentials() == null) {
            logger.debug("Loading security credentials from default credentials provider chain.");
            throw new AmazonClientException("Unable to find AWS security credentials.  "
                    + "Searched JVM system properties, OS env vars, and EC2 instance roles.  "
                    + "Specify credentials in Tomcat's context.xml file or put them in one of the places mentioned above.");
        }
        logger.debug("Using default AWS credentials provider chain to load credentials");
        return defaultChainProvider;

    }


}
