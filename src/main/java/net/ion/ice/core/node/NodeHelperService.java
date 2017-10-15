package net.ion.ice.core.node;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.SortField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("nodeHelperService")
public class NodeHelperService  {
    private Logger logger = LoggerFactory.getLogger(NodeHelperService.class);

    @Autowired
    private NodeService nodeService ;

    private Map<String, Date> lastChangedMap = new ConcurrentHashMap<>() ;

    public void  initSchema(String profile) throws IOException {
        saveResourceSchema("classpath:schema/core/*.json");
        saveResourceSchema("classpath:schema/core/*/*.json");
        try {
            saveResourceSchema("classpath:schema/core/datasource/" + profile + "/dataSource.json");
        }catch (Exception e){}

//        saveSchema("classpath:schema/node/*.json", lastChanged);
//        saveSchema("classpath:schema/node/**/*.json");
//        saveSchema("classpath:schema/test/*.json", lastChanged);
//        saveSchema("classpath:schema/test/**/*.json");

    }



    public void reloadSchema(String resourcePath) throws IOException {
        if(resourcePath.equals("node")){
            saveResourceSchema("classpath:schema/node/**/*.json");
        }else if(resourcePath.equals("test")){
            saveResourceSchema("classpath:schema/test/**/*.json");
        }else {
            saveFileSchema(resourcePath);
        }
    }

    private void saveFileSchema(String resourcePath) throws IOException {
        File[] files = new File(resourcePath).listFiles();
        for (File file : files) {
            if (file.getName().equals("nodeType.json")) {
                fileNodeSave(file);
            }
        }

        for (File file : files) {
            if (file.getName().equals("propertyType.json")) {
                fileNodeSave(file);
            }
        }

        for (File file : files) {
            if (file.getName().equals("event.json")) {
                fileNodeSave(file);
            }
        }
        for (File file : files) {
            if (file.getName().endsWith(".json") && !(file.getName().equals("nodeType.json") || file.getName().equals("propertyType.json") || file.getName().equals("event.json"))) {
                fileNodeSave(file);
            }
        }

        for (File file : files) {
            if(file.isDirectory()){
                saveFileSchema(file.getAbsolutePath());
            }
        }
    }

    private void fileNodeSave(File file) throws IOException {
        Collection<Map<String, Object>> nodeDataList = JsonUtils.parsingJsonFileToList(file) ;
        nodeDataList.forEach(data -> nodeService.saveNode(data));
    }


    public void saveResourceSchema(String resourcePath) throws IOException {
        saveSchema(resourcePath, true);
        saveSchema(resourcePath, false);
    }

    private void saveSchema(String resourcePath, boolean core) throws IOException {
        Resource[] resources = ApplicationContextManager.getResources(resourcePath);
        if(core) {
            for (Resource resource : resources) {
                if (resource.getFilename().equals("nodeType.json")) {
                    fileNodeSave(resource);
                }
            }

            for (Resource resource : resources) {
                if (resource.getFilename().equals("propertyType.json")) {
                    fileNodeSave(resource);
                }
            }

            for (Resource resource : resources) {
                if (resource.getFilename().equals("event.json")) {
                    fileNodeSave(resource);
                }
            }
        }else {
            for (Resource resource : resources) {
                if (!(resource.getFilename().equals("nodeType.json") || resource.getFilename().equals("propertyType.json") || resource.getFilename().equals("event.json"))) {
                    fileNodeSave(resource);
                }
            }
        }
    }

    private void fileNodeSave(Resource resource) throws IOException {
        String fileName = StringUtils.substringBefore(resource.getFilename(), ".json");
        Date last = new Date(resource.lastModified()) ;

        Collection<Map<String, Object>> nodeDataList = JsonUtils.parsingJsonResourceToList(resource) ;

        for(Map<String, Object> data : nodeDataList){
            String typeId = data.get(Node.TYPEID).toString() ;
            if(!lastChangedMap.containsKey(typeId)){
                Date changed = (Date) nodeService.getSortedValue(typeId, "changed", SortField.Type.LONG, true );
                lastChangedMap.put(typeId, changed) ;
            }

            Date changed = lastChangedMap.get(typeId) ;

            if(changed == null || changed.before(last)){
                nodeService.saveNode(data) ;
            }else{
                logger.info("After last schema : " + typeId);
            }
        }
    }
}
