package net.ion.ice.cjmwave.external.aws.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 18..
 */
@Service
public class S3Service {

    private Logger logger = Logger.getLogger(S3Service.class);

    @Autowired
    Environment env;

    private String bucketName;
    private String bucketKey;
    private String accessKey;
    private String secretKey;


    private AWSCredentials awsCredentials;
    private AmazonS3 s3Client;

    @PostConstruct
    public void init (){
        try {
            //Issue Credential
            //awsCredentials = new ProfileCredentialsProvider().getCredentials();
            bucketName = env.getProperty("aws.s3.bucketName");
            bucketKey = env.getProperty("aws.s3.bucketKey");
            accessKey = env.getProperty("aws.s3.accessKey");
            secretKey = env.getProperty("aws.s3.secretKey");

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
//        s3Client.setRegion(com.amazonaws.regions.Region.getRegion(Regions.AP_NORTHEAST_2));
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

    /*
    * Default 버킷키(경로) 에 서브패스 를 더한 s3 디렉토리 목록을 조회함
    * */
    public List retrieveObjectList(String subPath) throws Exception {
        if(subPath == null) subPath = "";

        String prefix = bucketKey
                 + ((subPath.length() > 0)  ? "/" + subPath : "");
        final ListObjectsV2Request req = new ListObjectsV2Request()
                        .withBucketName(bucketName)
                        .withMaxKeys(2);
        ListObjectsV2Result result = null;
        List<Map<String, Object>> s3FileInfo = new ArrayList<>();

        do {
            result = s3Client.listObjectsV2(bucketName, prefix);
            for (S3ObjectSummary objectSummary :
                    result.getObjectSummaries()) {
                Map<String, Object> singleFileInfo = new HashMap<String, Object>();
                long fSize = objectSummary.getSize();
                if(fSize > 0) {
                    singleFileInfo.put("filePath" , objectSummary.getKey());
                    singleFileInfo.put("fileSize" , objectSummary.getSize());
                    singleFileInfo.put("fileModified" , objectSummary.getLastModified());
                    s3FileInfo.add(singleFileInfo);
                } else {
                    // dir
                }
            }
//            System.out.println("Next Continuation Token : " + result.getNextContinuationToken());
            req.setContinuationToken(result.getNextContinuationToken());
        } while(result.isTruncated() == true );
        return s3FileInfo;
    }


    public List removeFiles(String path2target) throws Exception {
        List<Map<String, Object>> removed = new ArrayList<>();
        //권한이 ion 이외에도 지울 수 있으므로 지우는 부분은 하지 않는게 좋을지도..
        return removed;
    }
};
