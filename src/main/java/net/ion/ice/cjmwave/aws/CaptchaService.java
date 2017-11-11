package net.ion.ice.cjmwave.aws;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Created by leehh on 2017. 11. 11.
 */
@Service("captchaService")
public class CaptchaService {

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
}
