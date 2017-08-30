package net.ion.ice.service;

import org.apache.commons.net.util.Base64;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@Service("certificationService")
public class CertificationService {

    final static String apiKey = "ygoon!Z@X#C$Vqwer";

    public static String makeCertKey(String apiUrl){
        Long timeStamp = Long.valueOf(System.currentTimeMillis());
        return makeCertKey(apiUrl, apiKey, timeStamp);
    }

    public static String makeCertKey(String apiUrl, String apiKey, Long timeStamp){
        String signature = "";

        try{
            String apiAuthKey = apiKey + timeStamp.longValue() % 2L + timeStamp + apiUrl;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();
            signature = Base64.encodeBase64String(md.digest(apiAuthKey.getBytes("UTF-8"))).trim();

            if (apiUrl.contains("?"))
                apiUrl = apiUrl + "&";
            else {
                apiUrl = apiUrl + "?";
            }
        }catch(NoSuchAlgorithmException e1){
            e1.printStackTrace();
        }catch(UnsupportedEncodingException e2){
            e2.printStackTrace();
        }

        apiUrl = apiUrl + "tstamp=" + timeStamp + "&ssign=" + signature;
        return apiUrl;
    }






    public static int generateNumber() {
        return generateNumber(4);
    }

    //    특정 자리 수 난수 생성
    public static int generateNumber(int length) {

        String numStr = "1";
        String plusNumStr = "1";

        for (int i = 0; i < length; i++) {
            numStr += "0";

            if (i != length - 1) {
                plusNumStr += "0";
            }
        }

        Random random = new Random();
        int result = random.nextInt(Integer.parseInt(numStr)) + Integer.parseInt(plusNumStr);

        if (result > Integer.parseInt(numStr)) {
            result = result - Integer.parseInt(plusNumStr);
        }

        return result;
    }
}
