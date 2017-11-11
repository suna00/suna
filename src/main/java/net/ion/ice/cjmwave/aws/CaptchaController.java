package net.ion.ice.cjmwave.aws;

import net.ion.ice.core.response.JsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by leehh on 2017. 11. 11.
 */

@Controller
public class CaptchaController {
    @Autowired
    private CaptchaService captchaService;

    @RequestMapping("/checkCaptcha")
    @ResponseBody
    public Object check(@RequestParam String uuid){
        captchaService.checkSession(uuid);
        return JsonResponse.create();
    }

}
