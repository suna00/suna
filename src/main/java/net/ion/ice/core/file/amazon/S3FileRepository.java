package net.ion.ice.core.file.amazon;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.file.FileRepository;
import net.ion.ice.core.file.FileService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.file.TolerableMissingFileException;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

/**
 * Created by juneyoungoh on 2017. 9. 23..
 * fileHandler 를 s3 로 사용
 * 로컬에 쓰레기 데이터 누적을 막기 위해 파일 업로드 성공 / 실패 후 로컬 파일을 삭제함
 */

@Configuration
@ConfigurationProperties(prefix = "file.default")
public class S3FileRepository implements FileRepository {

    private Logger logger;

    @Autowired
    private Environment bootEnv;

    private String path ;           // 초기화 언제되는지 확인 필요함
    private File fileRoot ;

    private String bucketName;
    private String defaultBucketKey; //Full S3 path to a file
    private String accessKey;
    private String secretAccessKey;


    private AWSCredentials awsCredentials;
    private AmazonS3 s3Client;

    /*
    * 만약 업로드 다운로드 호출시, S3 클라이언트가 초기화되지 않았다면 별도로 호출하기 위해 뺌
    * 단, build() 메소드로 생성된 s3Client 는 immutable 객체임.
    * */
    private void initializeS3Client () throws Exception {
        awsCredentials = new BasicAWSCredentials(accessKey, secretAccessKey);
        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
        logger.info("bucketLocation with client :: " + s3Client.getBucketLocation(bucketName));
    }

    @PostConstruct
    private void initData(){
        logger = Logger.getLogger(S3FileRepository.class);
        try{
            bucketName = bootEnv.getProperty("aws.s3.bucketName");
            defaultBucketKey = bootEnv.getProperty("aws.s3.bucketKey");
            accessKey = bootEnv.getProperty("aws.s3.accessKey");
            secretAccessKey = bootEnv.getProperty("aws.s3.secretKey");

            // For TEST
            logger.info("S3 Information for the Application :: " + "\n"
                + "S3 bucketName :: " + bucketName + "\n"
                + "S3 bucketKey(root path) :: " + defaultBucketKey + "\n"
                + "S3 accessKey :: " + accessKey + "\n"
                + "S3 secretAccessKey :: " + secretAccessKey + "\n");

            initializeS3Client();

            fileRoot = new File(path) ;
            if(!fileRoot.exists()){
                fileRoot.mkdirs() ;
            }


            ApplicationContextManager.getBean(FileService.class).registerRepository("s3", this) ;
        } catch (Exception e) {
            logger.error("Unable to read S3 properties", e);
        }
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    /* S3 default path 조회 */
    public String getS3Path() {
        return String.valueOf(s3Client.getUrl(bucketName, defaultBucketKey));
    }

    /*
    ###################################################################################################
    ###################################################################################################
    ###################################################################################################

                                               LOGIC

    ###################################################################################################
    ###################################################################################################
    ###################################################################################################
    * */

    private String makeS3Path(String dirPath, File file ) throws Exception {
        String fileName = file.getName();
        String fullBucketPath = null;
        if(!dirPath.startsWith(defaultBucketKey)) {
            dirPath = defaultBucketKey + (dirPath.startsWith("/") ? dirPath : ("/" + dirPath));
        }

        if(!dirPath.endsWith(file.getName())) {
            fullBucketPath = dirPath + (dirPath.endsWith("/") ? dirPath : (dirPath + "/")) + fileName;
        } else {
            fullBucketPath = dirPath;
        }
        return fullBucketPath;
    }

    private String retrieveS3URL (String dirPath, File file) throws Exception {
        String fullBucketPath = makeS3Path(dirPath, file);
        URL bucketUrl = s3Client.getUrl(bucketName, fullBucketPath);
        return bucketUrl.toURI().toString();
    }

    private PutObjectResult uploadFile (String dirPath, File file) throws Exception {
        String fileName = file.getName();
        String fullBucketPath = makeS3Path(dirPath, file);
        logger.info("Send File [ " + fileName + " ] to S3(included FileName) [ " + fullBucketPath + " ]" );
        return s3Client.putObject(new PutObjectRequest(bucketName, fullBucketPath, file));
    }


    @Override
    public FileValue saveMutipartFile(PropertyType pt, String id, MultipartFile multipartFile) {
        String savePath =
                pt.getTid() + "/" +  pt.getPid()
                        + "/" + DateFormatUtils.format(new Date(), "yyyyMM/dd/") + UUID.randomUUID()
                        + "." + StringUtils.substringAfterLast(multipartFile.getOriginalFilename(), ".");
        File saveFile = null;
        try {
            saveFile = new File(fileRoot, savePath);
            if(!saveFile.getParentFile().exists()){
                saveFile.getParentFile().mkdirs() ;
            }
            multipartFile.transferTo(saveFile); // 이 경로에 떨어뜨리는 거 같은데..
            uploadFile(savePath, saveFile);
            savePath = retrieveS3URL(savePath, saveFile);
        } catch (Exception e) {
            logger.error("S3 MULTIPART FILE SAVE ERROR");
            throw new TolerableMissingFileException("S3 MULTIPART FILE SAVE ERROR : ", e);
        }
//        finally {
//            if(saveFile != null && saveFile.exists()) {
//                saveFile.delete();
//            }
//        }
        return new FileValue(pt, id, multipartFile, savePath) ;
    }

    @Override
    public FileValue saveFile(PropertyType pt, String id, File file, String fileName, String contentType) {
        String savePath = pt.getTid() + "/" +  pt.getPid() + "/" + DateFormatUtils.format(new Date(), "yyyyMM/dd/") + UUID.randomUUID() + "." + StringUtils.substringAfterLast(file.getName(), ".");
        File saveFile = new File(fileRoot, savePath) ;
        try {
            // 원격이니까 일부러 파일 경로 만들어 주지 않아도 됨
            FileUtils.copyFile(file, saveFile);
            uploadFile(savePath, file);
            savePath = retrieveS3URL(savePath, saveFile);
        } catch (Exception e) {
            logger.error("S3 FILE SAVE ERROR");
            throw new TolerableMissingFileException("S3 FILE SAVE ERROR : ", e);
        }
//        finally {
//            if(file != null && file.exists()) {
//                file.delete();
//            }
//        }
        return new FileValue(pt, id, file, savePath, fileName, contentType) ;
    }

    // 필요한지 모르겠음
    @Override
    public Resource loadAsResource(String path) {
        File file = new File(fileRoot, path) ;
        return new FileSystemResource(file);
    }

    @Override
    public FileValue saveResourceFile(PropertyType pt, String id, String path) {
        Resource res = ApplicationContextManager.getResource(path) ;
        String savePath =
                pt.getTid() + "/" +  pt.getPid()
                        + "/" + DateFormatUtils.format(new Date(), "yyyyMM/dd/") + UUID.randomUUID()
                        + "." + StringUtils.substringAfterLast(res.getFilename(), ".");
        File saveFile = null;
        int connectionTimeout = 1000, readTimeout = 5000;
        try{
            saveFile = new File(fileRoot, savePath) ;
            FileUtils.copyURLToFile(res.getURL(), saveFile, connectionTimeout, readTimeout);
            logger.info("SAVE RESOURCE FILE S3 :: " + saveFile.getCanonicalPath());
            uploadFile(savePath, saveFile);
            savePath = retrieveS3URL(savePath, saveFile);
        } catch (Exception e) {
            logger.error("S3 SAVE RESOURCE FILE SAVE ERROR");
            throw new TolerableMissingFileException("S3 SAVE RESOURCE FILE SAVE ERROR : ", e);
        }
//        finally {
//            if(saveFile != null && saveFile.exists()) {
//                saveFile.delete();
//            }
//        }
        return new FileValue(pt, res, savePath);
    }
}
