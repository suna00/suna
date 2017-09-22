package net.ion.ice.cjmwave.external.aws.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URL;

/**
 * Created by juneyoungoh on 2017. 9. 18..
 */
@Service
public class S3UploadService {

    private Logger logger = Logger.getLogger(S3UploadService.class);

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.s3.bucketKey}")
    private String bucketKey;

    @Value("${aws.s3.accessKey}")
    private String accessKey;

    @Value("${aws.s3.secretKey}")
    private String secretKey;

    private AWSCredentials awsCredentials;
    private AmazonS3 s3Client;

    @PostConstruct
    public void init (){
        try {
            //Issue Credential
            //awsCredentials = new ProfileCredentialsProvider().getCredentials();
            awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
            initializeS3Client();
        } catch (Exception e) {
            logger.error("Failed to initialize S3 storage", e);
        }
    }

    private void initializeS3Client () throws Exception {
        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();
        logger.info("bucketLocation with client :: " + s3Client.getBucketLocation(bucketName));
//        logger.info("String with client :: " + s3Client.getObjectAsString(bucketName, bucketKey));
        s3Client.setRegion(com.amazonaws.regions.Region.getRegion(Regions.AP_NORTHEAST_2));
    }


    public String uploadToS3 (String nodeTypeId, File file) {
        String rtn = null;
        try{
            if(s3Client == null) {
                initializeS3Client();
            }
            String fileName = file.getName();
            String fullBuckeyKey = bucketKey + "/mig/" + nodeTypeId + "/" + fileName;
            PutObjectResult pors = s3Client.putObject(new PutObjectRequest(bucketName, fullBuckeyKey, file));
            URL bucketUrl = s3Client.getUrl(bucketName, fullBuckeyKey);
            logger.info("Upload Result :: " + String.valueOf(pors));
            rtn = bucketUrl.toURI().toString() + "/mig/" + nodeTypeId + "/" + fileName;
        } catch (Exception e) {
            logger.info("S3 Upload Failed :: return null :: ", e);
        } finally {
            file.delete();
        }
        return rtn;
    }
};
