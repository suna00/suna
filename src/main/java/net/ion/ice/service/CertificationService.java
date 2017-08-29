package net.ion.ice.service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service("certificationService")
public class CertificationService {

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
