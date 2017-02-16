package net.ion.ice.configuration;

import net.ion.ice.response.JsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
