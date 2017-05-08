package net.ion.ice.core.configuration;

import net.ion.ice.core.response.JsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by jaehocho on 2017. 2. 9..
 */
@RestController
public class ConfigurationController {

    @Autowired
    private ConfigurationRepositoryService service ;


    @RequestMapping(value = "/config/{type}", method = RequestMethod.GET)
    public JsonResponse list(@PathVariable String type){
        return JsonResponse.create(service.list(type)) ;
    }
}
