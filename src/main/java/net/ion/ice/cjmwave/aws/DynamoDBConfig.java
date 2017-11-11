package net.ion.ice.cjmwave.aws;


import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import net.ion.ice.cjmwave.votePrtcptHst.ArtistVoteStatsByCntryTask;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by leehh on 2017. 11. 11.
 */
@Configuration
@EnableDynamoDBRepositories
        (basePackages = "com.baeldung.spring.data.dynamodb.repositories")
public class DynamoDBConfig {
    private static Logger logger = LoggerFactory.getLogger(DynamoDBConfig.class);

    @Value("${amazon.dynamodb.endpoint}")
    private String amazonDynamoDBEndpoint;

    @Value("${amazon.aws.accesskey}")
    private String amazonAWSAccessKey;

    @Value("${amazon.aws.secretkey}")
    private String amazonAWSSecretKey;

    @Value("${amazon.aws.regionId}")
    private String regionId;

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        AmazonDynamoDB amazonDynamoDB
                = new AmazonDynamoDBClient(amazonAWSCredentials());

        if (this.regionId != null) {
            amazonDynamoDB.setRegion(RegionUtils.getRegion(this.regionId));
        }

        if (!StringUtils.isEmpty(amazonDynamoDBEndpoint)) {
            amazonDynamoDB.setEndpoint(amazonDynamoDBEndpoint);
        }

        return amazonDynamoDB;
    }

    @Bean
    public AWSCredentials amazonAWSCredentials() {
        if(StringUtils.isNotEmpty(this.amazonAWSAccessKey)){
            return new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey);
        }
        AWSCredentialsProvider defaultChainProvider = new DefaultAWSCredentialsProviderChain();
        if (defaultChainProvider.getCredentials() == null) {
            logger.debug("Loading security credentials from default credentials provider chain.");
            throw new AmazonClientException("Unable to find AWS security credentials.  "
                    + "Searched JVM system properties, OS env vars, and EC2 instance roles.  "
                    + "Specify credentials in Tomcat's context.xml file or put them in one of the places mentioned above.");
        }
        logger.debug("Using default AWS credentials provider chain to load credentials");
        return defaultChainProvider.getCredentials();

    }

}
