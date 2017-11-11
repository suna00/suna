package net.ion.ice.cjmwave.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.tomcatsessionmanager.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.tomcatsessionmanager.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.tomcatsessionmanager.amazonaws.services.dynamodb.sessionmanager.converters.SessionConversionException;
import com.amazonaws.tomcatsessionmanager.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.util.IOUtils;
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
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * Created by leehh on 2017. 11. 11.
 */
@Service("captchaService")
public class CaptchaService {
    private static Logger logger = LoggerFactory.getLogger(CaptchaService.class);

    @Value("${amazon.dynamodb.endpoint}")
    private String amazonDynamoDBEndpoint;

    @Value("${amazon.aws.accesskey}")
    private String amazonAWSAccessKey;

    @Value("${amazon.aws.secretkey}")
    private String amazonAWSSecretKey;

    @Value("${amazon.aws.regionId}")
    private String regionId;



    private DynamoDBMapper dynamoDBMapper;

    private AmazonDynamoDBClient amazonDynamoDB;


    @PostConstruct
    public void initDynamoMapper(){
        this.amazonDynamoDB =  createDynamoClient();
        this.dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, (AWSCredentialsProvider) new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride("")));
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


    /**
     * 캡차 reset
     *
     * @param sessionKey 세션키
     * @return 일치 여부
     */

    public void reset(String sessionKey) {
        RequestContextHolder.getRequestAttributes().removeAttribute(sessionKey, RequestAttributes.SCOPE_SESSION);
    }


    public Session getSession(String sessionKey){
        DynamoSessionItem sessionItem = dynamoDBMapper.load(new DynamoSessionItem(sessionKey));
        if (sessionItem != null) {
            return toSession(sessionItem);
        } else {
            return null;
        }

    }

    public Session toSession(DynamoSessionItem sessionItem) {
        ObjectInputStream ois = null;
        try {
            ByteArrayInputStream fis = new ByteArrayInputStream(sessionItem.getSessionData().array());
            ois = new CustomObjectInputStream(fis, this.getClass().getClassLoader());

            StandardSession session = new StandardSession(null);
            session.readObjectData(ois);
            return session;
        } catch (Exception e) {
            throw new SessionConversionException("Unable to convert Dynamo storage representation to a Tomcat Session",
                    e);
        } finally {
            IOUtils.closeQuietly(ois, null);
        }
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
        if(StringUtils.isNotEmpty(this.amazonAWSAccessKey)){
            return (AWSCredentialsProvider) new StaticCredentialsProvider(new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey));
        }
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
